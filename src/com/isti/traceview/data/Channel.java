package com.isti.traceview.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.isti.jevalresp.RespUtils;
import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Station;

/**
 * Base class for channel representation, realize simplest SNCL logic, also holds response
 * 
 * @author Max Kokoulin
 */
public class Channel extends Observable implements Comparable<Object>, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum Sensor {
		SEISMIC, HYDROACUSTIC, INFRASONIC ,WEATHER, OTHER
	}
	
	public enum Status {
		DATA,
		DEAD_SENSOR,
		ZEROED_DATA,
		CLIPPED,
		CALIBRATION_UNDERWAY,
		EQUIPMENT_HOUSING_OPEN,
		DIGITIZING_EQUIPMENT_OPENED,
		VAULT_DOOR_OPENED,
		AUTHENTICATION_SEAL_BROKEN,
		EQUIPMENT_MOVED,
		CLOCK_DIFFERENTIAL_TOO_LARGE,
		GPS_RECEIVER_OFF,
		GPS_RECEIVER_UNLOCKED,
		DIGITIZER_INPUT_SHORTED,
		DIGITIZER_CALIBRATION_LOOP_BACK
	}

	private static final String fissuresPropFileName = "fissures.properties";

	private static final Logger logger = Logger.getLogger(Channel.class);

	private static Properties propsObj = null;

	private static List<Character> COMPDATA = null;

	/**
	 * The channel name.
	 */
	private String channelName;

	private Station station = null;

	/**
	 * The location name.
	 */
	private String locationName = null;

	/**
	 * The network name.
	 */
	private String networkName = null;

	private double sampleRate = 0.0;
	
	private transient boolean isSelected = false;
	
	private Sensor sensor = Sensor.SEISMIC;
	
	private Status status = Status.DATA;
	
	static {
		COMPDATA = new ArrayList<Character>();
		COMPDATA.add('Z');
		COMPDATA.add('N');
		COMPDATA.add('E');
		COMPDATA.add('1');
		COMPDATA.add('2');
	}

	/**
	 * Creates the channel information.
	 * 
	 * @param channelName
	 *            the channel name.
	 * @param networkName
	 *            the network name.
	 * @param station
	 *            the station.
	 * @param locationName
	 *            the location name
	 */
	public Channel(String channelName, Station station, String networkName, String locationName) {
		this.channelName = channelName.trim();
		this.station = station;
		if(networkName != null)
			this.networkName = networkName.trim();
		if(locationName != null)
			this.locationName = locationName.trim();
		station.addChannel(this);
	}

	/**
	 * Gets the channel name.
	 * 
	 * @return the channel name.
	 */
	public String getChannelName() {
		return channelName;
	}

	/**
	 * Gets the channel type. Type is last character of channel name.
	 * 
	 * @return channel type
	 */
	public char getType() {
		return getChannelName().substring(getChannelName().length() - 1).charAt(0);
	}

	/**
	 * Gets the location name.
	 * 
	 * @return the location name.
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * Gets the network name.
	 * 
	 * @return the network name.
	 */
	public String getNetworkName() {
		return networkName;
	}

	/**
	 * Getter of the property <tt>station</tt>
	 * 
	 * @return Returns the station.
	 */
	public Station getStation() {
		return station;
	}

	/**
	 * Setter of the property <tt>station</tt>
	 * 
	 * @param station
	 *            The station to set.
	 */
	public void setStation(Station station) {
		this.station = station;
	}

	/**
	 * Getter of sampleRate property
	 * 
	 * @return Sampling interval in milliseconds
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}

	@SuppressWarnings("unused")	
	private String[] getArray(String str) {
		String[] arr = new String[1];
		if (str == null || str.length() == 0) {
			arr[0] = "*";
		} else {
			arr[0] = str;
		}
		return arr;
	}

	/**
	 * Getter of the property <tt>response</tt>
	 * 
	 * @return Returns the channel response.
	 */
	public Response getResponse() throws TraceViewException {
		Response resp = TraceView.getDataModule().getResponse(getNetworkName(), getStation().getName(), getLocationName(), getChannelName());
		return resp;
	}
	
	public boolean isSelected(){
		return isSelected;
	}
	
	public void setSelected(boolean selected){
		this.isSelected = selected;
	}
	
	public Sensor getSensor() {
		return sensor;
	}

	public void setSensor(Sensor sensor) {
		this.sensor = sensor;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@SuppressWarnings("unused")
	private void loadProperties() {
		propsObj = new Properties();// System.getProperties();
		InputStream inStm = null;
		try {
			// open input stream to properites file:
			inStm = ClassLoader.getSystemResourceAsStream(fissuresPropFileName);

			// inStm = new BufferedInputStream(new FileInputStream(new File(fissuresPropFileName)));
			// load data from properties file:
			propsObj.load(inStm);
			// if CORBA properties not specified in loaded properites file
			// then put in values for ORBacus ORB:
			RespUtils.enterDefaultPropValue(propsObj, "org.omg.CORBA.ORBClass", "com.ooc.CORBA.ORB");
			RespUtils.enterDefaultPropValue(propsObj, "org.omg.CORBA.ORBSingletonClass", "com.ooc.CORBA.ORBSingleton");
		} catch (FileNotFoundException e) {
			StringBuilder message = new StringBuilder();
			message.append("Unable to open FISSURES property file \"" + fissuresPropFileName + "\":");
			logger.error(message.toString(), e);
			//throw new RuntimeException(e);
		} catch (IOException e) {
			StringBuilder message = new StringBuilder();
			message.append("Error loading FISSURES property file \"" + fissuresPropFileName + "\":");
			logger.error(message.toString(), e);
			//throw new RuntimeException(e);
		} finally {
			try {
				inStm.close();
			} catch (Exception ex) {
				// ignore any exceptions on close
				logger.error("Exception:", ex);
			}
		}
	}

	/**
	 * Special serialization handler
	 * 
	 * @param out
	 *            stream to serialize this object
	 * @see Serializable
	 * @throws IOException from ObjectInputStream.defaultReadObject()
	 * 
	 * @deprecated This method does not appear to be used by anything.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
        //logger.debug("== ENTER: Serializing " + toString());
		out.defaultWriteObject();
        //logger.debug("== EXIT");
	}

	/**
	 * Special deserialization handler
	 * -
	 * @param in
	 *            stream to deserialize object
	 * @see Serializable
	 * @throws IOException from ObjectInputStream.defaultReadObject()
	 * @throws ClassNotFoundException from ObjectInputStream.defaultReadObject()
	 * 
	 * @deprecated This method does not appear to be used by anything.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
        //logger.debug("== reading object --> in.defaultReadObject() Deserialize " + toString());
	}

	/**
	 * Returns a string representation of the channel in the debug purposes.
	 * 
	 * @return a string representation of the channel.
	 */
	public String toString() {
		return "Channel: " + getName();
	}

	/**
	 * Returns a string representation of the channel
	 * 
	 * @return a string representation of the channel.
	 */
	public String getName() {
		return getNetworkName() + "/" + getStation().getName() + "/" + getLocationName() + "/" + getChannelName();
	}

	/**
	 * Gets a hash code value for this station.
	 * 
	 * @return a hash code value for this station.
	 */
	public int hashCode() {
		return getNetworkName()==null?0:getNetworkName().hashCode() + getStation().getName().hashCode() + getChannelName().hashCode() + getLocationName()==null?0:getLocationName().hashCode();
	}

	/**
	 * Indicates whether some channel is equal to this one.
	 * 
	 * @return true if this station is the same as the one specified.
	 */
	public boolean equals(Object o) {
		if (o instanceof Channel) {
			Channel c = (Channel) o;
			return (getNetworkName().equals(c.getNetworkName()) && getStation().getName().equals(c.getStation().getName())
					&& getChannelName().equals(c.getChannelName()) && getLocationName().equals(c.getLocationName()));
		} else {
			return false;
		}
	}

	/**
	 * Default sorting order - according toString() and hashCode(), i.e Network - Station - Channel -
	 * Location Compares this object with the specified object. Returns a negative integer, zero, or
	 * a positive integer as this object is less than, equal to, or greater than the specified
	 * object.
	 * <p>
	 * 
	 * @param o
	 *            the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal
	 *         to, or greater than the specified object.
	 * @throws ClassCastException
	 *             if the specified object's type prevents it from being compared to this Object.
	 */
	public int compareTo(Object o) {
		if (o instanceof Channel) {
			Channel c = (Channel) o;
			return toString().compareTo(c.toString());
		} else {
			return 1;
		}
	}

	/**
	 * Compares channel types
	 * 
	 * @param type1
	 *            first type 
	 * @param type2
	 *            second type 
	 * @return compare result: a negative integer, zero, or a positive integer 
	 */
	static int channelTypeCompare(char type1, char type2) {
		if (type1 == type2) {
			return 0;
		} else {
			int type1pos = COMPDATA.indexOf(type1);
			int type2pos = COMPDATA.indexOf(type2);
			if (type1pos > type2pos) {
				return 1;
			} else {
				return -1;
			}
		}
	}

	/**
	 * Provide comparator according different channel sorting
	 * 
	 * @param sortOrder
	 *            configured channel sort type
	 * @return comparator according
	 */
	public static Comparator<Object> getComparator(Configuration.ChannelSortType sortOrder) {
		switch (sortOrder) {
		case TRACENAME:
			return new NameComparator();
		case CHANNEL:
			return new ChannelComparator();
		case CHANNEL_TYPE:
			return new ChannelTypeComparator();
		case NETWORK_STATION_SAMPLERATE:
			return new NetworkStationSamplerateComparator();
		case EVENT:
			return new EventComparator();
		default:
			return null;
		}
	}
}

/**
 * Comparator by channel string name, currently network - station - location - channel
 */
class NameComparator implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		if ((o1 instanceof Channel) && (o2 instanceof Channel)) {
			return (((Channel) o1).getChannelName()).compareTo(((Channel) o2).getChannelName());
		} else if ((o1 instanceof Channel) && !(o2 instanceof Channel)) {
			return 1;
		} else if (!(o1 instanceof Channel) && (o2 instanceof Channel)) {
			return -1;
		} else {
			return -1;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof NameComparator) {
			return super.equals(obj);
		} else {
			return false;
		}
	}
}

/**
 * Comparator by channel, i.e channel - network - station - location
 */
class ChannelComparator implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		if ((o1 instanceof Channel) && (o2 instanceof Channel)) {
			Channel channel1 = (Channel) o1;
			Channel channel2 = (Channel) o2;
			String ch1 = channel1.getName();
			String ch2 = channel2.getName();
			if (ch1.equals(ch2)) {
				String net1 = channel1.getNetworkName();
				String net2 = channel2.getNetworkName();
				if (net1.equals(net2)) {
					String st1 = channel1.getStation().getName();
					String st2 = channel2.getStation().getName();
					if (st1.equals(st2)) {
						return channel1.getLocationName().compareTo(channel2.getLocationName());
					} else {
						return st1.compareTo(st2);
					}
				} else {
					return net1.compareTo(net2);
				}
			} else {
				return ch1.compareTo(ch2);
			}
		} else if ((o1 instanceof Channel) && !(o2 instanceof Channel)) {
			return 1;
		} else if (!(o1 instanceof Channel) && (o2 instanceof Channel)) {
			return -1;
		} else {
			return -1;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof ChannelComparator) {
			return super.equals(obj);
		} else {
			return false;
		}
	}
}

/**
 * Comparator by channel type, i.e channel type - channel - network - station
 */
class ChannelTypeComparator implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		if ((o1 instanceof Channel) && (o2 instanceof Channel)) {
			Channel channel1 = (Channel) o1;
			Channel channel2 = (Channel) o2;
			char type1 = channel1.getType();
			char type2 = channel2.getType();
			if (type1 == type2) {
				String s1 = channel1.getName().substring(0, channel1.getName().length() - 1);
				String s2 = channel2.getName().substring(0, channel2.getName().length() - 1);
				if (s1.equals(s2)) {
					String net1 = channel1.getNetworkName();
					String net2 = channel2.getNetworkName();
					if (net1.equals(net2)) {
						String st1 = channel1.getStation().getName();
						String st2 = channel2.getStation().getName();
						if (st1.equals(st2)) {
							return channel1.getLocationName().compareTo(channel2.getLocationName());
						} else {
							return st1.compareTo(st2);
						}
					} else {
						return net1.compareTo(net2);
					}
				} else {
					return s1.compareTo(s2);
				}
			} else {
				return Channel.channelTypeCompare(type1, type2);
			}
		} else if ((o1 instanceof Channel) && !(o2 instanceof Channel)) {
			return 1;
		} else if (!(o1 instanceof Channel) && (o2 instanceof Channel)) {
			return -1;
		} else {
			return -1;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof ChannelTypeComparator) {
			return super.equals(obj);
		} else {
			return false;
		}
	}
}

/**
 * Comparator by network - station - sample rate - location code - channel type
 * return: {@literal 0=>Equal, 1=>(Obj1 > Obj2), -1=>(Obj1 < Obj2)}
 */
class NetworkStationSamplerateComparator implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		if ((o1 instanceof Channel) && (o2 instanceof Channel)) {
			Channel channel1 = (Channel) o1;
			Channel channel2 = (Channel) o2;
			String net1 = channel1.getNetworkName();
			String net2 = channel2.getNetworkName();
			if (net1.equals(net2)) {
				String st1 = channel1.getStation().getName();
				String st2 = channel2.getStation().getName();
				if (st1.equals(st2)) {
					Double sr1 = channel1.getSampleRate();
					Double sr2 = channel2.getSampleRate();
					if (sr1.equals(sr2)) {
						String loc1 = channel1.getLocationName();
						String loc2 = channel2.getLocationName();
						if (loc1.equals(loc2)) {
							char type1 = channel1.getType();
							char type2 = channel2.getType();
							int compareChan = Channel.channelTypeCompare(type1, type2);
							return compareChan;
						} else {
							int compareLoc = loc1.compareTo(loc2);
							return compareLoc;
						}
					} else {
						int compareSR = sr1.compareTo(sr2);
						return compareSR;
					}
				} else {
					int compareStat = st1.compareTo(st2);
					return compareStat;
				}
			} else {
				int compareNet = net1.compareTo(net2);
				return compareNet;
			}
		} else if ((o1 instanceof Channel) && !(o2 instanceof Channel)) {
			return 1;
		} else if (!(o1 instanceof Channel) && (o2 instanceof Channel)) {
			return -1;
		} else {
			return -1;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof NameComparator) {
			return super.equals(obj);
		} else {
			return false;
		}
	}
}

/**
 * Comparator by events
 */
class EventComparator implements Comparator<Object> {
	// ToDo EventComparator
	public int compare(Object o1, Object o2) {
		if ((o1 instanceof Channel) && (o2 instanceof Channel)) {
			return 0;
		} else if ((o1 instanceof Channel) && !(o2 instanceof Channel)) {
			return 1;
		} else if (!(o1 instanceof Channel) && (o2 instanceof Channel)) {
			return -1;
		} else {
			return -1;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof EventComparator) {
			return super.equals(obj);
		} else {
			return false;
		}
	}
}
