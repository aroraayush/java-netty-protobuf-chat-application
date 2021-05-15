# Netty Chat

In this lab, you'll build a simple chat client and server with Netty. To ensure 
compatibility across all the different student implementations in the class, we
will use Protocol Buffers to create our chat *wire format*.

You are given a .proto file and some starter code; it is your job to finish the
required functionality outlined below.


## Client

The basics are already done for you, so start here to familiarize yourself with
Netty. You will need to implement registration, private messages, and special 
handling for admin messages.

### Registration

The implementation you start with allows duplicate usernames, so adapt it to send
a registration message to the server. If registration fails (likely due to the
username already being taken), disconnect the client. This should be the **first**
thing a client does.

### Private Messages

If the user inputs a `/` followed by a username, send a private
message to that user. For example:

```
/matthew hey there! Netty is pretty cool, right?
```

Will be sent to the user 'matthew' only. The message needs to be routed through
the server since it will be able to map usernames to corresponding channels.

### Admin Messages

If the client receives an admin message, display it in a special way that can't
be confused as a regular message. Pretty simple!
  

## Server

The server is where non-blocking I/O with Netty really shines. We can have a
single thread handle a large number of client connections; after all, a chat
server is basically responsible for receiving messages from clients and then
broadcasting them to everyone else.

The server supports four message types:
* Registrations
* Basic messages (these should be sent to everyone)
* Private messages (these are sent to a specific channel)
* Admin messages (broadcast to all clients, originating on the server)

You may decide to use a Netty ChannelGroup to implement this, or it might be
more efficient to come up with your own strategy for broadcasting the
messages. Most of your implementation will reside in the `channelRead0`
method.

Hint: don't `sync` unless you have to. `sync` essentially makes your server
block, negating the performance benefits on non-blocking I/O.

# Grading

Since we are all using the same wire format, testing will be fun: you should
be able to handle several connections from other students in the class and
demonstrate client/server functionality is working correctly.

If this lab is too easy and you're looking for a challenge, write a benchmark
script that launches a large number of clients to determine what the upper
bound for client connections is.
