/**
 * Class DataFetcher allows for querying and interpretation of OneBusAway API for
 * bus stop and bus route data relavant to Bus Stop Alarm.
 * 
 * @author Michael Eng, David Nufer
 */

package com.busstopalarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class DataFetcher {
	// Location of REST API.
	private static final String HOST_NAME = "http://api.onebusaway.org/api/where/";
	
	// API Key needed for use of OneBusAway. Generated by OneBusAway.
	private static final String API_KEY = "v1_2XAu/HZkwK6PBLM63rgeRp34j5g=cXVhbmdodXkuZGFuZ0BnbWFpbC5jb20=";
	
	/**
	 * Retrieves a single stop element corresponding to the given stop id,
	 * and loads response data into returned BusStop
	 * @param stopId
	 * @return the BusStop corresponding to given stop id. Otherwise null if no corresponding stop.
	 */
	public BusStop getStopById(int stopId) throws IOException{
		//TODO: Currently a stub method.
		return null;
	}
	
	/**
	 * STUB METHOD. NOT FULLY IMPLEMENTED.
	 * Retrieves bus route data from OneBusAway and loads
	 * data into new BusRoute object. If getPolylinesAndStops is true,
	 * then polylines and bus stops are added as well.
	 * @param routeId
	 * @param getPolylinesAndStops flags whether or not to add polylines and stops
	 * @return BusRoute object loaded with retrieved data
	 * @throws IOException
	 */
	public BusRoute getBusRouteById(int routeId, boolean includePolylinesAndStops) throws IOException {
		BusRoute busRoute = new BusRoute();
		// TODO: Query OneBusAway for bus route data.
		
		// Good to get both polylines and stops at same time because 
		// data for them is in one big response from OneBusAway.
		if(includePolylinesAndStops) {
			JSONObject json = null;
			
			// HTTP GET request for the polylines in JSON format.
			try {
				URL url = new URL(HOST_NAME + "stops-for-route/1_" + routeId + ".json?key=" + API_KEY);
				System.out.println(url.toString());
				URLConnection connection = url.openConnection();
				InputStream inStream = connection.getInputStream();
				json = new JSONObject(convertStreamToString(inStream));
			} catch (IOException e) {
				System.out.println("Error: Connecting to the One Bus Away API.");
				throw e;
			} catch(JSONException e) {
				System.out.println("Error: Converting response into JSONObject");
				return null;
			}
			
			busRoute.setPolylines(getPolylinesParser(json));
			busRoute.setBusStops(getBusStopsForRouteParser(json));
		}
		
		return busRoute;
	}
	
	/**
	 * Retrieves the list of polylines for the given route.
	 * @param routeId is the bus route id of the route in Seattle.
	 * @return List of polylines containing the encoded polyline and the encoded level.
	 *         List is empty if there are now line for given route id.
	 */
	public List<Polyline> getPolylines(int routeId) {
		//TODO: Stub Method
		// Query OneBusAway and all getPolylinesParser.
		return null;
	}
	
	/** 
	 * Retrieves a list of stops corresponding to the given route id,
	 * represented as JSONObject rooted at key "stopGroupings".
	 * @param routeID
	 * @return list of stops as JSONObject, null if no such routeID.
	 */
	public List<BusStop> getBusStopsForRoute(int routeId) {
		//TODO: Stub Method
		// Query OneBusAway and call getBusStopsForRouteParser.
		return null;
	}
	
	/**
	 * Parses json response from OneBusAway api for polylines.
	 * @param json is the response from OneBusAway by a "Stops for a route" query.
	 * @return List of polylines containing the encoded polyline and the encoded level.
	 *         List is empty if there are now line for given route id.
	 */
	private List<Polyline> getPolylinesParser(JSONObject json) {
		ArrayList<Polyline> polylines = new ArrayList<Polyline>();
		
		// Retrieve the list of polylines from the JSON response.
		try {
			// Check to see if response code.
			String text = json.getString("text");
			if(!text.equals("OK")) {
				return polylines;
			}
			
			JSONObject data = json.getJSONObject("data");
			JSONArray polylineArray = data.getJSONArray("polylines");
			
			// Add all polylines to our collection.
			for(int i = 0; i < polylineArray.length(); i++) {
				if(!polylineArray.isNull(i)) {
					JSONObject polyline = polylineArray.getJSONObject(i);
					String line = polyline.getString("points");
					int length = polyline.getInt("length");
					polylines.add(new Polyline(line, length));
				}
			}
		} catch (Exception e) {
			System.out.println("Error getting polylines from json response.");
		}
		
		return polylines;
	}
	
	/**
	 * Parses json response from OneBusAway api for bus stops.
	 * @param json is the response from OneBusAway by a "Stops for a route" query.
	 * @return List of bus stops on the route.
	 */
	private List<BusStop> getBusStopsForRouteParser(JSONObject json) {
		// TODO: Currently a stub method.
		return null;
	}
	
	
	/**
	 * Converts an InputStream into a String
	 * @param inStream
	 * @return the converted string
	 * @throws IOException
	 */
	private String convertStreamToString(InputStream inStream) throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		if (inStream != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inStream, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				inStream.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}
}
