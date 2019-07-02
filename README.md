# PostToWS

Posts single line xml documents from stdin to a web service. One document is sent at a time to ensure the FIFO aspect.

Is especially useful in case of logfiles with lines and lines of (metadata and) xml.
Any metadata before the first `<` on each line will be ignored.

Speed has been improved by reading using 1 thread and sending using anoter.

Example usage:

`$ cat bunchofxmldocs.log|postToWS http://localserver:8080/service`

License: Apache 2.0: https://www.apache.org/licenses/LICENSE-2.0