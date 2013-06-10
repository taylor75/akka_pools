package sample.apps

import dlb.scheduler.tasks._

case class Add(nbr1: Int, nbr2: Int) extends Task

case class AddResult(nbr: Int, nbr2: Int, result: Int) extends TaskResult