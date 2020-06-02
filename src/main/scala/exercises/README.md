Quick questions
1. Can we assume any ordering of messages?
2. Aren't we causing race condition?
3. What does asynchronous actually mean for actors?
4. How does this all work?

Explanation:
1. Akka has a thread pool that it shares with actors.
2. Akka spawns a few threads(100s) and lots of actors(100000s per GB Heap).
3. Akka schedules actors for execution.
4. An actor is just a data structure which contains handler and a message queue(mailbox).
5. So an actor actually needs a thread to execute.


How does this communication with threads takes place?
1. Sending a message
- message is enqueued in the actor's mailbox
- thread safe.

2. Processing a message
- a thread is scheduled to run this actor.
- messages are extracted from the mailbox, in order.
- the thread invokes the handler on each message.

What guarantees are we getting?
- Only one thread operates on an actor at any time.
- actors are effectively single-threaded.
- no locks needed.





