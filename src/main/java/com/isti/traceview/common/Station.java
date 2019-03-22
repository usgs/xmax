package com.isti.traceview.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.isti.traceview.data.Channel;

/**
 * Defines the station information. Station list is initialized during startup: such information as
 * longitude, latitude, elevation and depth is loaded from a station configuration file. Class
 * implements an interface "Comparable" to define sort order in the station lists.
 */
public class Station implements Comparable<Object>, Serializable {
	private static final long serialVersionUID = 1L;

	private String name = "";

	private String network = "";

	private transient String longName = "";

	private transient double latitude = 0;

	private transient double longitude = 0;

	private transient double elevation = 0;

	private transient double depth = 0;

	//private transient TimeInterval presence = null;

	private transient Set<Channel> channels = null;

	/**
	 * Creates the station information.
	 * 
	 * @param stationName
	 *            the station name.
	 * @param network
	 *            name of network station belongs
	 * @param stationLongName
	 *            the station long name.
	 * @param stationLatitude
	 *            the station latitude.
	 * @param stationLongitude
	 *            the station longitude.
	 * @param elevation
	 *            station elevation according sea level
	 * @param depth
	 *            depth of station placement
	 * @param channels
	 *            the collection of 'IPlotDataProviderInfo' objects, references to channels of this
	 *            station
	 */
	public Station(String stationName, String network, String stationLongName, double stationLatitude, double stationLongitude, double elevation,
			double depth, Set<Channel> channels) {
		this.name = stationName.trim();
		this.network = network;
		this.longName = stationLongName;
		this.latitude = stationLatitude;
		this.longitude = stationLongitude;
		this.elevation = elevation;
		this.depth = depth;
		this.channels = channels;
	}

	/**
	 * Simplified constructor
	 * 
	 * @param stationName
	 *            the station name.
	 */
	public Station(String stationName) {
		this(stationName, null, null, 0.0, 0.0, 0.0, 0.0, new HashSet<>());
	}

	/**
	 * Getter of the property <tt>name</tt>
	 * 
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setter of the property <tt>name</tt>
	 * 
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Getter of the property <tt>network</tt>
	 * 
	 * @return Returns the network.
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * Setter of the property <tt>network</tt>
	 * 
	 * @param network
	 *            The network to set.
	 */
	public void setNetwork(String network) {
		this.network = network;
	}

	/**
	 * Getter of the property <tt>longName</tt>
	 * 
	 * @return Returns the longName.
	 */
	public String getLongName() {
		return longName;
	}

	/**
	 * Setter of the property <tt>longName</tt>
	 * 
	 * @param longName
	 *            The longName to set.
	 */
	public void setLongName(String longName) {
		this.longName = longName;
	}

	/**
	 * Getter of the property <tt>channels</tt>
	 * 
	 * @return Returns the channels.
	 */
	public Set<Channel> getChannels() {
		return channels;
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	/**
	 * Setter of the property <tt>latitude</tt>
	 * 
	 * @param latitude
	 *            The latitude to set.
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Getter of the property <tt>latitude</tt>
	 * 
	 * @return Returns the latitude.
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Getter of the property <tt>longitude</tt>
	 * 
	 * @return Returns the longitude.
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Setter of the property <tt>longitude</tt>
	 * 
	 * @param longitude
	 *            The longitude to set.
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Getter of the property <tt>elevation</tt>
	 * 
	 * @return Returns the elevation.
	 */
	public double getElevation() {
		return elevation;
	}

	/**
	 * Setter of the property <tt>elevation</tt>
	 * 
	 * @param elevation
	 *            The elevation to set.
	 */
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	/**
	 * Getter of the property <tt>depth</tt>
	 * 
	 * @return Returns the depth.
	 */
	public double getDepth() {
		return depth;
	}

	/**
	 * Setter of the property <tt>depth</tt>
	 * 
	 * @param depth
	 *            The depth to set.
	 */
	public void setDepth(double depth) {
		this.depth = depth;
	}

	/**
	 * Compares this object with the specified object. Returns a negative integer, zero, or a
	 * positive integer as this object is less than, equal to, or greater than the specified object.
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
		return getName().compareTo(((Station) o).getName());
	}

	/**
	 * Indicates whether some station is equal to this one.
	 * 
	 * @return true if this station is the same as the one specified.
	 */
	public boolean equals(Object o) {
		return o instanceof Station && getName().equals(((Station) o).getName());
	}

	/**
	 * Gets a hash code value for this station.
	 * 
	 * @return a hash code value for this station.
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/**
	 * Returns a string representation of the station.
	 * 
	 * @return a string representation of the station.
	 */
	public String toString() {
		return name;
	}

	/**
	 * Special serialization handler
	 * 
	 * @param out
	 *            stream to serialize this object
	 * @see Serializable
	 * @throws IOException if thrown when writing the ObjectOutputStream to file.
	 * @deprecated this method is not called by anything
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	/**
	 * Special deserialization handler
	 * 
	 * @param in
	 *            stream to deserialize object
	 * @see Serializable
	 * @throws IOException if thrown when reading the ObjectOutputStream.
	 * @deprecated this method is not called by anything
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		channels = new TreeSet<>();
	}
}
