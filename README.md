akka_pools
==========

Exploring akka remote modules with a focus on simplifying the job of achieving pools of distributed akka workers

## Getting Started ... now featuring akka-cluster-experimental!
1. Start the calculator sample app scheduler
  1.1 Start an sbt window for the sample calculator app task scheduler with ```sbt -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false```
  1.2 Start the scheduler ```run-main sample.remote.calculator.CalculatorTaskScheduler -sp 2551```
2. Start the other cluster seed node (a remote calculator app worker pool)
  2.1 Start an sbt window for the sample calculator app task scheduler with ```sbt -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false```
  2.2 Start the remote node containing calculator actors ```run-main sample.remote.calculator.RemoteCalculatorPoolApp -l RemoteCalculatorPoolApp -sp 2552```
3. Shutdown now can be managed via the jmx jconsole gui app.
  3.1 In a separate window, simply type ```jconsole``` and double click on one of the jvms from sbt.

* Note: It is now possible to start a RemoteWorkerPool for the calculator app on any accessible server and have it join the cluster and start doing calculations.  Just change the remote.netty.host in application.conf appropriately.  It is also no longer necessary to specify a port. ```run-main sample.remote.calculator.RemoteCalculatorPoolApp -l RemoteCalculatorPoolApp``` will just work.
