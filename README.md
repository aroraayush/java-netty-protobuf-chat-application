# Peer to Peer Netty Chat

A simple chat client and server with Netty with Protocol Buffers as chat *wire format*.

The system handles several connections from other peers in the same network and demonstrates client/server functionality.

## Server

The server is where non-blocking I/O with Netty really shines. We can have a single thread handle a large number of client connections; after all, a chat server is basically responsible for receiving messages from clients and then broadcasting them to everyone else.

The server supports four message types:
- **Registrations**
- **Basic** (these should be sent to everyone)
- **Private messages** (these are sent to a specific channel)
- **Admin messages** (broadcast to all clients, originating on the server)

- Used Netty **ChannelGroup** to implement this. Most of the implementation resides in the `channelRead0` method.

- One cannot use `sync` unless one has to as `sync` essentially makes the server block, negating the performance benefits on non-blocking I/O.

## Client

Implementation of registration, private messages, and special 
handling for admin messages.

1. ### Registration
Once a client joins the network, it should send a registration message to the server, proiving its usersname, else we could have users with same usernames. This could create a issue while sending Private messages.

If registration fails (likely due to the username already being taken), the client is disconnect. 

The **username** can be chosen on a **first-come first-server** basis.

2. ### Private Messages

If the user inputs a `/` followed by a username, a private 
message is sent to that user. For example:

```
/user1 hey there! Netty is pretty cool, right?
```
will be sent to the user 'user1' only. 
The message needs is routed through the server since it will be able to map usernames to corresponding channels.

3. ### Admin Messages

If the client receives an admin message, it is in a special way that can't be confused as a regular message.

---
## Future Scope

Writing a benchmark script that launches a large number of clients to determine the upper bound for client connections on server.
