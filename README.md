# stream-splitter

 This repo has a simple multi-part file uploader to demonstrate a stream splitting utility.
 The utility splits a single input stream into 1, 2 (or more) separate input streams for multiple recipients
 to avoid the need for "resetting" a stream, or buffering the entire contents of the stream on the server.

 The consumers of the stream run on separate threads and in this case are actually REST calls to delegate
 services.  The REST delegates pass the stream on through without buffering the entire contents based on how
 the RestTemplate is configured
