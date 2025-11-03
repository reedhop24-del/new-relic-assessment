# New Relic Assessment

### Prerequisites
- Java 22
- Maven 3.9.9 or higher

### Building locally
In one terminal window, run the following command to start the backend server:

`mvn clean compile exec:java -Dexec.mainClass=com.newRelic.assessment.server.Server -DskipTests`

Once the server is running open a window and run the tests under:

`mvn -Dtest=SocketTest test`

Then the shutdown test suite under:

`mvn -Dtest=ShutdownTest test`

Tests must be run in that order because the shutdown test shuts down the server. Any other process running on port 4000 
will cause the app to error out so ensure port 4000 is free before starting the server.

### Assumptions
- The problem description did not specify when there are 5 connected clients, what is the expected behavior of the 6th client. 
  Rather than reject that client, the client will be queued by the executor service when a thread is available. The client is not 
  necessarily aware that they are waiting and may time out, though I thought this was better behavior than rejecting the client.

- Input that does not have a terminating new line character is ignored and the connection remains open. In contrast, input that 
  is invalid (non integer chars or length greater than 9) the connection is closed.

- When a termination message is received from the client, the server closes all client connections and does a clean shutdown 
  meaning it shuts down and gracefully, handles any exceptions, and closes all resources it had open.

- Unique numbers are stored only in memory during runtime, they are not reloaded from disk if the server restarts.

- The server only listens on 4000 port as specified in the requirements rather than having it be configurable and passed 
  at runtime.

- Printing to standard output the report of change made it impossible to unit test that functionality, so that was manually tested
  rather than automated.

- For simplicity’s sake using the Standard output for logging, since a requirement was to log a report of change using the 
  standard output I just extended it to be used at all times.

- Junit 5 is used for testing as it improved simplicity and readability of the project.
