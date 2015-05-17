package edu.asu.irs13;

import java.awt.*; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

@SuppressWarnings("serial")
public class Assign3UI extends JFrame
{
	JButton search ; 
	JTextField queryField;
	JLabel queryF;
	JEditorPane htmlPane;
	JPanel container;
	JScrollPane scrPane;
	JLabel time;

	Assign3UI(String title) throws Exception
	{
	super( title );
	
	search = new JButton(" Search ");
	queryField = new JTextField(45);
	time = new JLabel();
	queryF = new JLabel("Enter the query here");

	container = new JPanel();
	scrPane = new JScrollPane(container);

	htmlPane = new JEditorPane("text/html",retLoadStr());
	
	add(scrPane);
	htmlPane.setEditable(false);
	container.setLayout( new FlowLayout() );
	container.setPreferredSize( new Dimension(750, 1400) );
	container.add(htmlPane);
	
	search.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) 
        {
        	String query = queryField.getText();
        	String test;
        	double startTime;
        	int nInt = 50;
        	int kInt = 10;
        	try
        	{
        		startTime = System.currentTimeMillis();
        		test = Assign3.uiInterface(query, nInt, kInt);
        		time.setText("The time taken for the query is : " + (System.currentTimeMillis()-startTime)/1000 + "s");
        		htmlPane.setText(test);
        		repaint();
        	}
        	catch(Exception exp){}
            repaint(); 
        }
    });
	
	setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
	
	private String retLoadStr()
	{
		String html = "<html><head><script source=http://www.public.asu.edu/~vjayabal/olderfiles/assets/bootstrap.js></script>";
		html += "<script source='http://www.public.asu.edu/~vjayabal/olderfiles/assets/jquery-2.1.1.js'></script>";
		html += "<link href='http://www.public.asu.edu/~vjayabal/olderfiles/assets/bootstrap.css' rel='stylesheet'></head>";
		html += "<body bgcolor='#dfdfdf'>";
		html += "<font size='20'>&nbsp;&nbsp;&nbsp; . . .<br>Loading</font></body></html>";
		return html;
	}
	
	public static void main ( String[] args )throws Exception
	  {
		Assign3UI frm = new Assign3UI("CSE 494 : Information Retrieval : Assignment 3");
		frm.setSize( 800, 700 );
	    frm.setVisible( true );   
	    
	    Assign3.init();
		Assign3.preCompute();
		
		frm.htmlPane.setText("<html></html>");
		frm.container.remove(frm.htmlPane);
		frm.container.add( frm.queryF );
		frm.container.add( frm.queryField );
		frm.container.add( frm.search );
		frm.container.add( frm.time );
		frm.container.add( frm.htmlPane );
		frm.repaint();
	  }
}
