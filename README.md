akka_pools
==========

Exploring akka remote modules with a focus on simplifying the job of achieving pools of distributed akka workers

## Getting Started
1. Build and package the dependencies by executing the ```./dist.sh``` command then ```cd dist```.
2. Start the master task dispatching actor with ```./cluster-admin CalculatorTaskScheduler -s s01 -sp 8001 -sh localhost```
3. Start a remote pool of (sample app) CalculatorWorkers ```./cluster-admin RemoteCalculatorPoolApp -l L01 -s s01 -sp 8001 -sh localhost -wp 8004```.  Note that many of these could be started on multiple remote hosts, just be sure they point to the scheduler correctly, and that they are named uniquely.
4. Shutdown the remote worker pools gracefully with ```./cluster-admin ShutdownWorkerPool -ah localhost -sn RemoteCalculatorPoolApp -an L01 -ap 8004 -ac workercluster```

