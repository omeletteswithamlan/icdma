package mtu.construction.gui.old;

//a new class is needed to make a dialog modal only in one thread
//since the normal setModal(true) forces dialog boxes to be modal
//across threads, we want the dialog to be modal across only this thread

public class ModalThread extends Thread
{
	private boolean killed = false;
	
	public ModalThread()
	{
		super();
	}
	
	public void run()
	{
		while(!killed)
			yield();
	}
	
	public void die()
	{
		killed = true;
	}
	
	public void start()
	{
		super.start();
		
		try
		{
			this.join();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
}
