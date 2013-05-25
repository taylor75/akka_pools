package dlb.scheduler.tasks

trait Task

case class ExpireRemotePool()

case class NeedWork() extends Task

case class Setup() extends Task

case class TaskComplete(addResult:TaskResult, executionTimeInSecs:Double) extends TaskResult

case class TaskExecutionError(e:Throwable)

trait TaskResult

