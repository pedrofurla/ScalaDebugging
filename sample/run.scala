import debug._
import scala.collection.JavaConversions._

object run {
	object debug extends Debugger("javadebug")	
	import debug._
	def main(args:Array[String]) = {
		val clazz=classByName("debug.test.Dumbclass").get
		//println("availableStrata:" + clazz.availableStrata)
		//println("threads:"+threads)
		//debug.eventSetCallback = { e => println("eventCallback(size "+e.size+"):"+e) }
		
		val b=clazz.breakAtLine(15)
		val m=clazz.fieldByName("cont").watchModification;
				
		debug.breakpointCallback = { e => 				
			println("breakpoint reached:"+e)
			Thread.sleep(7000)
			e.stepLine
			println("breakpoint stepped:"+e)
			b.disable()
			vm.resume()
		}
		
		debug.watchModificationCallback = { e =>
			println("modified to:"+e.valueToBe)
			//vm.resume()
		}
		
		Thread.sleep(7000)
				
		val mt = mainThread.get
				
		java.lang.Thread.sleep(40000)
		println("Ended")
		vm.resume
		()
	}
}
