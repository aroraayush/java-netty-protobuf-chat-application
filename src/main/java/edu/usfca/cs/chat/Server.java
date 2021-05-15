package edu.usfca.cs.chat;

import edu.usfca.cs.chat.ChatMessages.AdminMessage;
import edu.usfca.cs.chat.ChatMessages.PrivateMessage;
import edu.usfca.cs.chat.ChatMessages.ChatMessage;
import edu.usfca.cs.chat.ChatMessages.Registration;
import edu.usfca.cs.chat.ChatMessages.ChatMessagesWrapper;
import edu.usfca.cs.chat.net.ServerMessageRouter;
import io.netty.channel.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@ChannelHandler.Sharable
public class Server
    extends SimpleChannelInboundHandler<ChatMessagesWrapper> {

    ServerMessageRouter messageRouter;
    private HashMap<String,ChannelHandlerContext> userChannels;

    private int port;

    public Server(int port) {
        this.port = port;
        userChannels = new HashMap<>();
    }

    public void start(int port) throws IOException {
        messageRouter = new ServerMessageRouter(this);
        messageRouter.listen(port);
        System.out.println("Listening on port " + port + "...");
    }

    public static void main(String[] args)
    throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: Server <port>");
            System.exit(1);
        }

        Server s = new Server(Integer.parseInt(args[0]));
        s.start(s.port);
        while (true){
            System.out.println("Enter an admin message : ");
            s.sendAdminMessage();
        }
    }

    private void sendAdminAlertMessage(ChannelHandlerContext ctx, String message) {
        ChatMessagesWrapper.Builder wrapperBuilder =  ChatMessagesWrapper.newBuilder();
        if(ctx != null){
            wrapperBuilder.setAdminMessage(
                    AdminMessage.newBuilder()
                            .setMessageBody(message)
                            .build()
            );
            ChannelFuture write = ctx.writeAndFlush(wrapperBuilder.build());
            write.syncUninterruptibly();
        }
        else{
            sendAdminMessagetoAllUsers(message);
        }
    }
    private void sendAdminMessage() {

        String message = "";

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));) {

            while (true) {
                try {
                    message = reader.readLine();

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if(message.trim().length() == 0){
                    System.out.println("Please enter a valid message");
                }
                else{
                    sendAdminMessagetoAllUsers(message);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendAdminMessagetoAllUsers(String message) {

        ChatMessagesWrapper.Builder wrapperBuilder =  ChatMessagesWrapper.newBuilder();

        for(String username : userChannels.keySet()){
            ChannelHandlerContext context = userChannels.get(username);
            wrapperBuilder.setAdminMessage(
                    AdminMessage.newBuilder()
                            .setMessageBody(message)
                            .build()
            );
            ChannelFuture write = context.channel().writeAndFlush(wrapperBuilder.build());
            write.syncUninterruptibly();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        /* A connection has been established */
        InetSocketAddress addr
            = (InetSocketAddress) ctx.channel().remoteAddress();
        System.out.println("Connection established: " + addr);
        Integer port = addr.getPort();
        messageRouter.listen(port);
    }

    private void sendExistingClientNames() {
        Set<String> users = userChannels.keySet();
        String respMessage = "Users available on server: " + String.join(", ", users);
        sendAdminAlertMessage(null, respMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        /* A channel has been disconnected */
        InetSocketAddress addr
            = (InetSocketAddress) ctx.channel().remoteAddress();
        String username = null;
        for(String user : userChannels.keySet()){
            if(userChannels.get(user).equals(ctx)){
                username = user;
                userChannels.remove(user);
                break;

            }
        }
        System.out.println("Connection lost: " + addr);
        sendExistingClientNames();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx)
    throws Exception {
        /* Writable status of the channel changed */
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ChatMessagesWrapper msg) {

        String sourceUser = null;

        if(msg.hasPrivateMessage()) {
            PrivateMessage privateMessage = msg.getPrivateMessage();
            sourceUser = privateMessage.getMessageContents().getUsername();
            String message = privateMessage.getMessageContents().getMessageBody();

            String[] privateMsgArr = message.trim().split(" ");
            if(privateMsgArr.length == 1){
                String errMessage = "Incorect private message usage: " + System.lineSeparator() + "Syntax: /<destination_user> <message>";
                sendAdminAlertMessage(ctx, errMessage);
            }
            else{
                String destinationUser = privateMsgArr[0].substring(1); // Removing /
                if (!userChannels.containsKey(destinationUser)) {
                    String errMessage = "No user with username '" + destinationUser + "' found";
                    sendAdminAlertMessage(ctx, errMessage);
                }
                else{
                    String finalMessage = message.substring(destinationUser.length()+2);  // removing /destinationUser and 1 space
                    sendPrivateMessage(sourceUser, destinationUser, finalMessage);
                }
            }
        }
       else if(msg.hasChatMessage()) {
            ChatMessage message = msg.getChatMessage();
            sourceUser = message.getUsername();

            if (!userChannels.containsKey(sourceUser)) {
            System.out.println("No user with username " + sourceUser + " has registered yet");
            return;
            }
            sendChatMessage(sourceUser, msg);
       }
       else if(msg.hasRegistration()) {
            sourceUser = msg.getRegistration().getUsername();

            // Username already exist
            if(userChannels.containsKey(sourceUser)){
                // Sending a blank message back, informing user that username already exists
                String responseMessage = "User with username " + sourceUser + " already exists. Please change your username and retry";
                sendAdminAlertMessage(ctx, responseMessage);
                System.out.println("User [" + sourceUser + "] trying to register, but username already exists");
                ctx.close();
            }
            else{
                userChannels.put(sourceUser, ctx);
                System.out.println("User [" + sourceUser + "] registered with the server");
                sendExistingClientNames();
            }
       }
    }

    private void sendPrivateMessage(String sourceUser, String destinationUser, String finalMessage) {

        Channel destinationChannel = userChannels.get(destinationUser).channel();
        InetSocketAddress addr
                = (InetSocketAddress) destinationChannel.remoteAddress();
        int destinationPort = addr.getPort();
        String destinationHost = addr.getHostName();

        ChatMessage chatMessage = ChatMessage.newBuilder()
                .setUsername(sourceUser)
                .setMessageBody(finalMessage)
                .build();

        PrivateMessage privateMessage = PrivateMessage.newBuilder()
                .setDestinationHost(destinationHost)
                .setDestinationPort(destinationPort)
                .setMessageContents(chatMessage)
                .build();

        ChatMessagesWrapper wrapper =
                ChatMessagesWrapper.newBuilder()
                .setPrivateMessage(privateMessage)
                .build();

        ChannelFuture write = destinationChannel.writeAndFlush(wrapper);
        write.syncUninterruptibly();
    }

    private void sendChatMessage(String sourceUser, ChatMessagesWrapper msg) {
        ChatMessagesWrapper.Builder msgWrapper = ChatMessagesWrapper.newBuilder();
        ChatMessage message = msg.getChatMessage();

        for(String user : userChannels.keySet()){
            // Sending chat message to every client except source
            if(!user.equalsIgnoreCase(sourceUser)){
                ChatMessage message_obj = ChatMessage.newBuilder()
                        .setUsername(sourceUser)
                        .setMessageBody(message.getMessageBody())
                        .build();
                msgWrapper.setChatMessage(message_obj);
                Channel userChannel = userChannels.get(user).channel();
                ChannelFuture write = userChannel.writeAndFlush(msgWrapper.build());
                write.syncUninterruptibly();
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
