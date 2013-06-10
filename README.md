akka_pools
==========

Exploring akka remote/cluster modules with a focus on simplifying the job of achieving pools of distributed akka workers

## Getting Started
- Start the calculator sample app scheduler
        
        sbt -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
        
        > run-main sample.apps.CalculatorTaskScheduler 2551

    
- Start the other cluster seed node (a remote calculator app worker pool)

        sbt -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false

        > run-main sample.apps.RemoteCalculatorPoolApp 2552
    
- Shutdown now can be managed via the jmx jconsole gui app, open a new window and type ```jconsole``` and double click on one of the jvms from sbt.  Navigate the akka entry within the MBeans tab to find the node url you'd like to shut down.  Then go to operations and paste that url into "leave" operation textfield.
 
* Note: It is now possible to start a RemoteWorkerPool for the calculator app on any accessible server and have it join the cluster and start doing calculations.  Just change the ```akka.remote.netty.host``` in ```application.conf``` appropriately.  It is also no longer necessary to specify a port. 
  * ```sbt run-main sample.apps.RemoteCalculatorPoolApp``` will just work.

