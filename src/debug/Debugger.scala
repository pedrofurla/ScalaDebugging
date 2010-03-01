
package debug

import com.sun.jdi._
import scala.collection.JavaConversions._

class Debugger(val address:String) {
	val connector = 
		(Bootstrap.virtualMachineManager().attachingConnectors())
		.filter(_.name.equals("com.sun.jdi.SharedMemoryAttach"))
		.first.asInstanceOf[connect.AttachingConnector]
	
	val vm:VirtualMachine = { 
		val args=connector.defaultArguments()
		val nameArg=args.get("name")
		nameArg.setValue(address)
		val vm = connector.attach(args)
		vm.resume
		vm
	}
	
	val connected:Boolean=true
		
	/** Always invoked listener */
	var eventSetCallback:(event.EventSet => Unit) = { e=> () } // TODO _?
	
	var breakpointCallback:(event.BreakpointEvent => Unit) = { e => () } // TODO _?
	
	var watchModificationCallback:(event.ModificationWatchpointEvent => Unit) = { e => () } // TODO _?
		
	val eventDispatcher = new Thread("Debugger EventDispatcher") {
		setDaemon(true)
		override def run():Unit = {
			while(connected) {
				val set=vm.eventQueue.remove()
				// TODO should be called in another thread?
				eventSetCallback(set)
				for(e <- set) e match {
					case bpe:event.BreakpointEvent => breakpointCallback(bpe)
					case mwe:event.ModificationWatchpointEvent => watchModificationCallback(mwe) 
					case _ => println("unknown event:"+e)
				}					
			}
		}
	}
	eventDispatcher.start()
	
	val erm = vm.eventRequestManager
	
	def threads = vm.allThreads
	def mainThread = threads.find(_.name == "main")
	
	def classByName(fqn:String):Option[ReferenceType] = {
		val classes = vm.classesByName(fqn)
		if(classes.size!=1) None
		else Option(classes.get(0));
	}
		
	implicit def rt2Helper(rt:ReferenceType) = new ReferenceTypeHelper(rt)
	implicit def l2Helper(l:Location) = new LocationHelper(l)
	implicit def bpe2Helper(l:event.BreakpointEvent) = new BreakpointEventHelper(l)
	implicit def f2Helper(f:Field) = new FieldHelper(f)
		
	class ReferenceTypeHelper(rt:ReferenceType) {
		def line(l:Int) = asBuffer(rt.allLineLocations()).filter(l == _.lineNumber)	
		def firstAtLine(l:Int) = line(l).first
		def breakAtLine(l:Int) = firstAtLine(l).breakpoint
	}
	
	class FieldHelper(f:Field) {
		import com.sun.jdi.request.EventRequest._
		def watchModification = {
			val req = erm.createModificationWatchpointRequest(f);
			req.setSuspendPolicy(SUSPEND_NONE)
			req.enable()
			req
		}
	}	
	
	class LocationHelper(l:Location) {
		def breakpoint = erm.createBreakpointRequest(l);
	}
	
	class BreakpointEventHelper(bpe:event.BreakpointEvent) {		
		import com.sun.jdi.request.StepRequest._
		import com.sun.jdi.request.EventRequest._
		val vm = bpe.virtualMachine
		val erm = vm.eventRequestManager
		
		def stepLine = {
			val step = erm.createStepRequest(bpe.thread,STEP_LINE,STEP_OVER)
			step.setSuspendPolicy(SUSPEND_EVENT_THREAD)
			step.addCountFilter(1)
			step.enable()
			vm.resume()
		}
	}
		
}


