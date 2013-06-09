package dlb.scheduler.tasks

trait Task

case class Expire()

case object NeedWork extends Task

case class TaskComplete(addResult:TaskResult, executionTimeInSecs:Double) extends TaskResult

case class TaskExecutionError(e:Throwable)

trait TaskResult

case class JobFailed(reason: String, job: Task)

case object BackendRegistration
