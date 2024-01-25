/**
 * 
 */
package report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import core.ConnectionListener;
import core.DTNHost;
import core.DTNSim;
import core.Settings;
import core.SimClock;
import report.ConnectionsReport.Connection.ActiveConnectionException;

/**
 * <p>Sampling report that reports information of a contact between two nodes. 
 * The connection information gathered during a sample interval is: 
 * contact time: How long a connection lasts, 
 * inter-contact time: Elapsed time until a new connection,
 * number of contacts per host   
 * number of one-time contact per host pair
 * number of muliple-time contacts per host pair
 */
public class ConnectionsReport extends SamplingReport implements ConnectionListener {
	
	/** Setting for the number of sample interval cycles used to report the connection information.
	 * By default is {@value #SAMPLE_INTERVAL_CYCLES_DEF}.  
	 */
	public static final String N_SAMPLE_INTERVAL_CYCLES_S = "nrofSampleIntervalCycles";
	
		
	/** 
		Setting for the decay applied to the node centrality through the 
		function decay = f(t)= {@value}^t. By default is {@value #CENT_DECAY_GAMMA_S}
	*/
	public static final String CENT_DECAY_GAMMA_S = "centDecayGamma";
	
	/** Default value for the setting {@value}. */
	public static final int SAMPLE_INTERVAL_CYCLES_DEF = 1;
	
	
	/** Default value for the setting {@value #CENT_DECAY_GAMMA_S} */
	public static final double CENT_DECAY_GAMMA_DEF = 0.9;
	
	/** Number of sample interval cycles used to report the connection information (default {@value #SAMPLE_INTERVAL_CYCLES_DEF}.). */
	private int sampleIntervalCycles;
		
	/** Decay applied to the node centrality through the function decay = f(t)= {@value}^t (default ).*/
	private double centDecayGamma; 
	
	/** Map to store the connected hosts and the time the connection has last. */
	private HashMap<Connection, Connection> activeConnections;
	
	/** Array indexed by the intervalCycle, containing the stablished connections for a sample interval */
	private List<List<Connection>> connectionsSampled;
	
	/** Current sampling cycle. Each cycle is of {@value SamplingReport.interval} seconds */
	private int currentSampleCycle = 0;

	
//	/** List that stores the contact-time (time while the two hosts are in range) of a contact. 
//	 * The max size of the list is {@value #CENT_DECAY_GAMMA_S}*/
//	private List<Double> contactTimeList = new ArrayList<Double>();
//	
//	/** List that stores the the inter-contact times between two consecutive connections (h1->h2). 
//	 * The information is added once by pair of hosts where (h1->h2) equals (h2-h1).*/
//	private List<Double> interContactTimeList = new ArrayList<Double>();
	
	
	
	/**
	 * Constructor
	 */
	public ConnectionsReport() {
		Settings settings = getSettings();
		this.sampleIntervalCycles = (settings.contains(N_SAMPLE_INTERVAL_CYCLES_S)) ? 
				settings.getInt(N_SAMPLE_INTERVAL_CYCLES_S) : SAMPLE_INTERVAL_CYCLES_DEF;
		this.centDecayGamma = (settings.contains(CENT_DECAY_GAMMA_S)) ? 
				settings.getDouble(CENT_DECAY_GAMMA_S) : CENT_DECAY_GAMMA_DEF;		

		init();
	}
	
	@Override
	protected void init() {
		super.init();
		this.activeConnections = new HashMap<Connection, Connection>();
		this.connectionsSampled = new ArrayList<List<Connection>>();
		this.connectionsSampled.add(0, new ArrayList<Connection>());
	}
	
	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}
		this.addConnection(host1, host2);
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// TODO Auto-generated method stub
		newEvent();
		Connection ac = removeConnection(host1, host2);

		if (ac == null) {
			return; /* the connection was started during the warm up period */
		}

		ac.connectionEnd();
	}

	
	protected void addConnection(DTNHost host1, DTNHost host2) {
		Connection ac = new Connection(host1, host2);

		assert !activeConnections.containsKey(ac) : "Already contained "+
			" a connection of " + host1 + " and " + host2;

		this.activeConnections.put(ac,ac);
		this.connectionsSampled.get(this.currentSampleCycle).add(ac);		
	}

	/**
	 * Removes the connection from the connections map.
	 * @param host1
	 * @param host2
	 * @return the removed connection.
	 */
	protected Connection removeConnection(DTNHost host1, DTNHost host2) {
		Connection ac = new Connection(host1, host2);
		ac = this.activeConnections.remove(ac);
		return ac;
	}
	
	/**
	 * Method called automatically each sample interval. If the 
	 * interval belongs to the warmup period no data is sampled.
	 * The sampled data consists of the connections established during
	 * the last sampling cycle. This method increases current sample cycle
	 * and creates an empty connections list to track the new connections 
	 * during the sampling. 
	 * @param hosts all the hosts in the simulation.
	 */
	protected void sample(List<DTNHost> hosts) {
		//TODO: fer el sampleig de la finestra de connectionsHistory que toqui.
		if(!isWarmup()) {
			this.connectionsSampled.add(++this.currentSampleCycle, new ArrayList<Connection>());
		}
	}
	
	
	@Override
	public void done() { 
		//write((i*this.granularity) + " " + contacts);
		for (int i=0, sampleCycles = this.connectionsSampled.size(); i < sampleCycles; i++) {
			Map<DTNHost, DTNHost> sampleEvaluatedHosts = new HashMap<DTNHost, DTNHost>();
			for (Connection connection : this.connectionsSampled.get(i)) {
				if(!sampleEvaluatedHosts.containsKey(connection.getContact().getH1())) {
					
				}
			}
		}
		
		super.done();
	}


	//nested classes
	//====================================================================================
	// Active Connection Class
	//====================================================================================
	/**
	 * Class that contains the information of an active connection between two hosts. 
	 */
	protected class Connection{
		/** Active contact between two nodes */
		private Contact contact;
		private double startTime;
		private double endTime;
		
		public Connection(DTNHost h1, DTNHost h2){
			this.contact = new Contact(h1, h2);
			this.startTime = getSimTime();
			this.endTime = -1;
		}
		
		/**
		 * Method that checks if a host participates in a connection.
		 * @param h host Host to be checked if participates in the connection.
		 * @return <code>true/false</code> whether the host participates or not 
		 * in the connection.  
		 */
		public boolean contains(DTNHost h) {
			return this.contact.contains(h);
		}
		
		/**
		 * Returns true if the other connection info contains the same hosts.
		 */
		public boolean equals(Object other) {
			return this.contact.equals(other);					
		}
		
		/**
		 * Returns the same hash for ConnectionInfos that have the
		 * same two hosts.
		 * @return Hash code
		 */
		public int hashCode() {
			return this.contact.hashCode();
		}
		
		/**
		 * Should be called when the connection ended to record the time.
		 * Otherwise {@link #getConnectionTime()} will use end time as
		 * the time of the request.
		 */
		public void connectionEnd() {
			this.endTime = getSimTime();
		}

		/**
		 * Returns the time that passed between creation of this info
		 * and call to {@link #connectionEnd()}. If connectionEnd() has not 
		 * been called by the time this method is called, an exception is launched.
		 * @return The amount of simulated seconds passed between creation of
		 * this info and calling connectionEnd()
		 * @throws ActiveConnectionException if the two hosts are still connected.
		 */
		public double getConnectionTime() throws ActiveConnectionException {
			if (this.endTime == -1) {
				throw new ActiveConnectionException();
			}
			else {
				return this.endTime - this.startTime;
			}
		}
		
		/**
		 * @return Returns the {@link #contact} of the connection.
		 */
		public Contact getContact() {
			return this.contact;
		}
		
		//inner inner class
		/**
		 * Exception class to indicate that the two hosts are still connected.
		 */
		public class ActiveConnectionException extends Exception{

			public ActiveConnectionException() {
				super();
			}
			
			public ActiveConnectionException(DTNHost h1, DTNHost h2) {
				super(String.format("The connection between %s and %s is still active", h1, h2));
			}
			
		}
	}
	
	//====================================================================================
	// Active Contact Class
	//====================================================================================
	
	/**
	 * Class that represents a bidirectional connection between two nodes
	 */
	protected class Contact{
		private DTNHost h1;
		private DTNHost h2;

		public Contact(DTNHost h1, DTNHost h2) {
			this.h1 = h1;
			this.h2 = h2;			
		}
		
		//getters

		public DTNHost getH1() {
			return h1;
		}

		public DTNHost getH2() {
			return h2;
		}
		
		/**
		 * Checks whether the host intervenes in the connection.
		 * @param host
		 * @return <code>true</code> in case the host participates in the 
		 * connection. False otherwise. 
		 */
		public boolean contains(DTNHost host) {
			return (host.equals(this.h1) || host.equals(this.h2));
		}
		
		/**
		 * Returns true if the other connection info contains the same hosts.
		 */
		public boolean equals(Object other) {
			if (!(other instanceof Contact)) {
				return false;
			}

			Contact c = (Contact)other;
			
			// considering bidirectional connections
			return ((this.h1 == c.h1 && this.h2 == c.h2) || (this.h1 == c.h2 && this.h2 == c.h1)) ? true : false;
		}


		/**
		 * Returns the same hash for ConnectionInfos that have the
		 * same two hosts.
		 * @return Hash code
		 */
		public int hashCode() {
			String hostString;

			if (this.h1.compareTo(this.h2) < 0) {
				hostString = this.h1.toString() + "-" + this.h2.toString();
			}
			else {
				hostString = this.h2.toString() + "-" + this.h1.toString();
			}

			return hostString.hashCode();
		}
		
	}
	
	//====================================================================================
	// Sample Class
	//====================================================================================
	/**
	 * Data structure that wraps the collected information of a host along one sampling interval.
	 */
	protected class HostSample{
		
		/** Time when the sample was taken*/
		private double timeStamp;
		
		/** Host involved in a connection established during this sample interval. */
		private DTNHost host;
		
		/** Connections started during this interval. */
		private List<Connection>  sampleConnections;
		
		/** Subset of the connections {@link #sampleConnections} list where 
		 * {@link #host} has taken part either as origin or destination. */
		private List<Connection> hostConnections;
		
		/** Average time the host connetions last. */
		private double connectionTimeAvg;
		
		private double intercontactTimeAvg;
				
		/** 
		 * @param host The host we are taking the snapshot from
		 * @param sampleConnections Connections started during this interval.
		 */
		public HostSample(double timeStamp, DTNHost host, List<Connection> sampleConnections) {
			this.timeStamp = timeStamp;
			this.host = host;
			this.sampleConnections = sampleConnections;
			this.setHostConnections();
			this.setConnectionTimeAvg();
		}
		
		private void setHostConnections(){
			this.hostConnections = new ArrayList<Connection>();
					
			for (Connection conn: this.sampleConnections) {
				if (conn.contains(this.host)) {
					this.hostConnections.add(conn);
				}
			}
		}

		/**
		 * Sets the property {@link #connectionTimeAvg} with the connection-time 
		 * (the time a connection lasts) average of the host's connections.
		 * @return An average of the connection-times.
		 */
		private void setConnectionTimeAvg() {
			int finishedConn = 0;
			double connectionTimeAcum = 0;
			
			for(Connection conn : this.hostConnections) {
				try {
					connectionTimeAcum+=conn.getConnectionTime();
					finishedConn++;
				} catch (ActiveConnectionException e) {}
			}
			this.connectionTimeAvg = connectionTimeAcum/finishedConn;
		}

		/**
		 * @return the standard deviation of the connectionTimes. 
		 */
		public double getConnectionTimeDeviation() {
			int finishedConn = 0;
			double varianceAcum = 0;
			
			for(Connection conn : this.hostConnections) {
				try {
					  varianceAcum+=Math.pow(this.connectionTimeAvg - conn.getConnectionTime(),2);
					  finishedConn++;
				} catch (ActiveConnectionException e) {}
			}
			return Math.sqrt(varianceAcum)/finishedConn;
		}
		
		/**
		 * Sets the inter-contact time average of all the host connections.
		 * (inter-contact time: time between two consecutive contacts) 
		 */
		public void setInterContactTimeAvg() {
			int hostConnectionsSize = this.hostConnections.size();
			double interContactTimeAcum = 0;
			if(hostConnectionsSize == 0) {
				this.intercontactTimeAvg = 0;
			}else if(hostConnectionsSize == 1) {
				this.intercontactTimeAvg = this.hostConnections.get(0).startTime;
			}else { //At least there are two elements in the list
				List<Double> interContactTimes =  this.hostConnections.stream()
						.map(conn -> conn.startTime ).sorted().collect(Collectors.toList());
				for(int i=1; i< hostConnectionsSize;i++) {
					interContactTimeAcum += (interContactTimes.get(i) - interContactTimes.get(i-1));
				}
				this.intercontactTimeAvg = interContactTimeAcum/(hostConnectionsSize - 1);
			}
		}
		
		
		
		/**
		 * Calculates the average of the elements in a list.
		 * @param elements The list with the elements to be averaged.
		 * @return The element's list average.
		 */
//		private static double getAvg(List<Double> elements) {
//            return elements.stream().mapToDouble(d -> d)
//            		.average()
//            		.orElse(0.0);
//		}
		
		/**
		 * Calculates the standard deviation of the elements 
		 * @param elements The list with the elements which standard 
		 * deviation we want to calculate.
		 * @return The standard deviation of the elements in the list.
		 */
//		private static double getDeviation(List<Double> elements) {
//			double avg = HostSample.getAvg(elements);
//						
//			return Math.sqrt(elements.stream().mapToDouble(d -> Math.pow(d-avg, 2)).average().orElse(0.0));
//		}
//		
		/**
		 * Returns an String with the connections involving the host regarding  
		 * to the sampleConnections: timestamp, HostName, contact-times_avg, 
		 * contact-times_deviation, inter-contact-times, 
		 * inter-contact-times_deviation, nrofcontacts, nrof-single-contacts, 
		 * nrof-multiple-contacts, centrality. 
		 */
		public String toString() {
			return String.format("%.1f, %s, %.1f, %.1f", this.timeStamp, 
					this.host, this.connectionTimeAvg, this.getConnectionTimeDeviation());
		}
		
	}
	
	// =================================================================================================================
	// end nested classes
	// =================================================================================================================

}
