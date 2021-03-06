package pt.up.fe.socialcrowd.API;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import pt.up.fe.socialcrowd.logic.*;

public abstract class Request {
	public static final int POST = 2;
	public static final int GET = 1;
	public static final int PUT = 3;
	public static final int DELETE = 4;
	
	private static final String API_ROOT_URL = "https://gnomo.fe.up.pt/~ei10043/slim/api";
	private static final String API_SESSIONS_URL = API_ROOT_URL + "/sessions";
	private static final String API_EVENTS_URL = API_ROOT_URL + "/events";
	private static final String API_COMMENTS_URL = API_ROOT_URL + "/comments";
	private static final String API_VOTES_URL = API_ROOT_URL + "/votes";
	private static final String API_RATINGS_URL = API_ROOT_URL + "/ratings";
	private static final String API_SUBSCRIPTIONS_URL = API_ROOT_URL + "/subscriptions";
	
	private static final double radius = 5000;
	
	private static List<NameValuePair> createSessionHeader(String sid) {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("SESSION_ID",sid));
		return l;
	}

	private static JSONObject doRequest(String url, int type, List<NameValuePair> nameValuePairs, List<NameValuePair> headers) throws InvalidParameterException, IllegalStateException, IOException, JSONException {
		HttpClient httpclient = new DefaultHttpClient();
		if (type == GET && nameValuePairs != null) {
			url += '?';
			for (int i = 0; i < nameValuePairs.size(); i++) {
				if (i != 0) {
					url += "&";
				}
				url += URLEncoder.encode(nameValuePairs.get(i).getName(), "UTF-8") + "=" + URLEncoder.encode(nameValuePairs.get(i).getValue(), "UTF-8");
			}
		}

		HttpResponse response = null;

		try {
			switch (type) {
			case POST:
				HttpPost httppost = new HttpPost(url);
				if (nameValuePairs != null && !nameValuePairs.isEmpty())
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
				if (headers != null && !headers.isEmpty()) {
					for (int i = 0; i < headers.size(); i++) {
						httppost.addHeader(headers.get(i).getName(),headers.get(i).getValue());
					}
				}
				response = httpclient.execute(httppost);
				break;
			case GET:
				Log.i("doRequest()", url);
				HttpGet httpget = new HttpGet(url);
				if (headers != null && !headers.isEmpty()) {
					for (int i = 0; i < headers.size(); i++) {
						httpget.addHeader(headers.get(i).getName(),headers.get(i).getValue());
					}
				}
				response = httpclient.execute(httpget);
				break;
			case DELETE:
				HttpDelete httpdelete = new HttpDelete(url);
				if (headers != null && !headers.isEmpty()) {
					for (int i = 0; i < headers.size(); i++) {
						httpdelete.addHeader(headers.get(i).getName(),headers.get(i).getValue());
					}
				}
				response = httpclient.execute(httpdelete);
				break;
			case PUT:
				HttpPut httpput = new HttpPut(url);
				if (nameValuePairs != null && !nameValuePairs.isEmpty())
					httpput.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
				if (headers != null && !headers.isEmpty()) {
					for (int i = 0; i < headers.size(); i++) {
						httpput.addHeader(headers.get(i).getName(),headers.get(i).getValue());
					}
				}
				response = httpclient.execute(httpput);
				break;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (response == null || response.getStatusLine().getStatusCode() != 200) {
			HttpEntity responseEntity = response.getEntity();
			if(responseEntity!=null) {
			    Log.i("Request.doRequest", EntityUtils.toString(responseEntity));
			}
			throw new IllegalArgumentException();
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = in.readLine();
		return new JSONObject(json);
	}

	public static Session createSession(String token, String login, String password) throws IllegalStateException, IOException, JSONException, RequestException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("login",login));
		l.add(new BasicNameValuePair("password",password));
		List<NameValuePair> tokenl = new ArrayList<NameValuePair>();
		tokenl.add(new BasicNameValuePair("TOKEN",token));

		JSONObject object = doRequest(API_SESSIONS_URL,POST,l,tokenl);

		String result = object.getString("result");
		if (result.equals("success")) {
			JSONObject data = object.getJSONObject("data");
			return new Session(data.getString("sid"),data.getInt("user_id"));
		}
		else {
			throw new RequestException(object.getString("data"));
		}
	}

	public static void deleteSession(String session_id, String token) throws JSONException, RequestException, InvalidParameterException, IllegalStateException, IOException {
		List<NameValuePair> tokenl = new ArrayList<NameValuePair>();
		tokenl.add(new BasicNameValuePair("TOKEN",token));
		JSONObject object = doRequest(API_SESSIONS_URL + "/" + session_id,DELETE,null,tokenl);

		String result = object.getString("result");
		if (!result.equals("success"))
			throw new RequestException(object.getString("data"));
	}

	public static DetailedEvent getEventByID(int eventid) throws JSONException, IllegalStateException, IOException, RequestException, ParseException {		
		JSONObject object = doRequest(API_EVENTS_URL + "/" + Integer.toString(eventid),GET,null,null);
		String result = object.getString("result");
		if (result.equals("success")) {
			return DetailedEvent.parseJSON(object.getJSONObject("data"));			 
		}
		else
			throw new RequestException(object.getString("data"));
	}

	public static ArrayList<BaseEvent> getEvents() throws IllegalStateException, IOException, JSONException, RequestException, ParseException {
		JSONObject object = doRequest(API_EVENTS_URL,GET,null,null);

		Log.i("Request.getEvents()", object.toString());
		
		String result = object.getString("result");
		if (result.equals("success")) {
			ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
			JSONArray arrayevents = object.getJSONArray("data");
			for (int i = 0 ;i < arrayevents.length(); i++) {
				events.add(BaseEvent.parseJSON(arrayevents.getJSONObject(i)));
			}
			return events;		 
		}
		else
			throw new RequestException(object.getString("data"));
	}

	public static DetailedEvent createEvent(String session_id, int type, String name, String description, 
			Date start_date, Date end_date, Location location, ArrayList<String> tags, String category) throws RequestException, InvalidParameterException, IllegalStateException, IOException, JSONException, ParseException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("name",name));
		l.add(new BasicNameValuePair("description",description));
		l.add(new BasicNameValuePair("category",category));
		l.add(new BasicNameValuePair("start_date",DateParser.parseDate(start_date)));
		l.add(new BasicNameValuePair("end_date",DateParser.parseDate(end_date)));
		JSONObject locationobj = new JSONObject();
		locationobj.put("name", location.getText());
		if (location.getGps() != null)
			locationobj.put("gps", location.getGps());
		l.add(new BasicNameValuePair("location",locationobj.toString()));
		JSONArray arraytags = new JSONArray(tags);
		JSONObject objtags = new JSONObject();
		objtags.put("tags", arraytags);
		l.add(new BasicNameValuePair("tags",objtags.toString()));
		if (type == BaseEvent.PUBLIC)
			l.add(new BasicNameValuePair("type","public"));
		else if (type == BaseEvent.PRIVATE)
			l.add(new BasicNameValuePair("type","private"));
		else if (type == BaseEvent.GEOLOCATED)
			l.add(new BasicNameValuePair("type","geolocated"));
		else
			throw new RequestException("invalid event type");

		JSONObject object = doRequest(API_EVENTS_URL,POST,l,createSessionHeader(session_id));

		String result = object.getString("result");
		if (result.equals("success")) {
			return DetailedEvent.parseJSON(object.getJSONObject("data"));
		}
		else
			throw new RequestException(object.getString("data"));
	}

	public static void deleteEvent(String session_id, int eventid) throws JSONException, RequestException, InvalidParameterException, IllegalStateException, IOException {
		JSONObject object = doRequest(API_EVENTS_URL + "/" + eventid,DELETE,null,createSessionHeader(session_id));

		String result = object.getString("result");
		if (!result.equals("success"))
			throw new RequestException(object.getString("data"));
	}

	public static Comment createComment(String session_id, int eventid, String text, Date date) throws JSONException, ParseException, InvalidParameterException, IllegalStateException, IOException, RequestException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("eventid",Integer.toString(eventid)));
		l.add(new BasicNameValuePair("input",text));
		l.add(new BasicNameValuePair("created_at",DateParser.parseDate(date)));

		JSONObject object = doRequest(API_COMMENTS_URL,POST,l,createSessionHeader(session_id));

		String result = object.getString("result");
		if (result.equals("success")) {
			return Comment.parseJSON(object.getJSONObject("data"));
		}
		else
			throw new RequestException(object.getString("data"));
	}

	public static Comment editComment(String session_id, int id, String text) throws JSONException, ParseException, InvalidParameterException, IllegalStateException, IOException, RequestException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("input",text));

		JSONObject object = doRequest(API_COMMENTS_URL + "/" + Integer.toString(id),PUT,l,createSessionHeader(session_id));

		String result = object.getString("result");
		if (result.equals("success")) {
			return Comment.parseJSON(object.getJSONObject("data"));
		}
		else
			throw new RequestException(object.getString("data"));
	}

	public static void deleteComment(String session_id, int id) throws JSONException, RequestException, InvalidParameterException, IllegalStateException, IOException {
		JSONObject object = doRequest(API_COMMENTS_URL + "/" + Integer.toString(id),DELETE,null,createSessionHeader(session_id));

		String result = object.getString("result");
		if (!result.equals("success"))
			throw new RequestException(object.getString("data"));
	}

	public static Vote createVote(String session_id, int comment_id, int type) throws JSONException, InvalidParameterException, IllegalStateException, IOException, RequestException {

		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("comment",Integer.toString(comment_id)));
		
		if (type == Vote.UPVOTE)
			l.add(new BasicNameValuePair("type",Integer.toString(1)));
		else if (type == Vote.DOWNVOTE)
			l.add(new BasicNameValuePair("type",Integer.toString(-1)));
		else
			throw new RequestException("invalid type");

		JSONObject object = doRequest(API_VOTES_URL,POST,l,createSessionHeader(session_id));

		String result = object.getString("result");
		if (result.equals("success")) {
			if (type == Vote.UPVOTE)
				return Upvote.parseJSON(object.getJSONObject("data"));
			else
				return Downvote.parseJSON(object.getJSONObject("data"));
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static void deleteVote(String session_id, int id) throws JSONException, RequestException, InvalidParameterException, IllegalStateException, IOException {
		JSONObject object = doRequest(API_VOTES_URL + "/" + Integer.toString(id),DELETE,null,createSessionHeader(session_id));

		String result = object.getString("result");
		if (!result.equals("success"))
			throw new RequestException(object.getString("data"));
	}
	
	public static Rating createRating(String session_id, int eventid, int value) throws InvalidParameterException, IllegalStateException, IOException, JSONException, RequestException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("eventid",Integer.toString(eventid)));
		
		JSONObject object = doRequest(API_RATINGS_URL,POST,l,createSessionHeader(session_id));

		String result = object.getString("result");
		if (result.equals("success")) {
			return Rating.parseJSON(object.getJSONObject("data"));
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static void deleteRating(String session_id, int id) throws InvalidParameterException, IllegalStateException, IOException, JSONException, RequestException {

		JSONObject object = doRequest(API_RATINGS_URL + "/" + Integer.toString(id),DELETE,null,createSessionHeader(session_id));

		String result = object.getString("result");
		if (!result.equals("success"))
			throw new RequestException(object.getString("data"));
	}

	public static Subscription createSubscription(String session_id, int eventid, int user_id, Date created_at) throws InvalidParameterException, IllegalStateException, IOException, JSONException, RequestException, ParseException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		l.add(new BasicNameValuePair("eventid",Integer.toString(eventid)));
		l.add(new BasicNameValuePair("user_id",Integer.toString(user_id)));
		l.add(new BasicNameValuePair("created_at",DateParser.parseDate(created_at)));
		
		JSONObject object = doRequest(API_SUBSCRIPTIONS_URL,POST,l,createSessionHeader(session_id));

		String result = object.getString("result");
		if (result.equals("success")) {
			return Subscription.parseJSON(object.getJSONObject("data"));
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static void deleteSubscription(String session_id, int id) throws InvalidParameterException, IllegalStateException, IOException, JSONException, RequestException {

		JSONObject object = doRequest(API_SUBSCRIPTIONS_URL + "/" + Integer.toString(id),DELETE,null,createSessionHeader(session_id));

		String result = object.getString("result");
		if (!result.equals("success"))
			throw new RequestException(object.getString("data"));
	}
	
	public static ArrayList<BaseEvent> getEventsByOwnerID(int ownerid) throws IllegalStateException, IOException, JSONException, RequestException, ParseException {
		JSONObject object = doRequest(API_EVENTS_URL+"?owner_id="+Integer.toString(ownerid),GET,null,null);

		String result = object.getString("result");
		if (result.equals("success")) {
			ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
			JSONArray arrayevents = object.getJSONArray("data");
			for (int i = 0 ;i < arrayevents.length(); i++) {
				events.add(BaseEvent.parseJSON(arrayevents.getJSONObject(i)));
			}
			return events;		 
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static ArrayList<BaseEvent> getEventsBySubscriberID(int userid) throws IllegalStateException, IOException, JSONException, RequestException, ParseException {
		JSONObject object = doRequest(API_EVENTS_URL+"?subscriber_id="+Integer.toString(userid),GET,null,null);

		String result = object.getString("result");
		if (result.equals("success")) {
			ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
			JSONArray arrayevents = object.getJSONArray("data");
			for (int i = 0 ;i < arrayevents.length(); i++) {
				events.add(BaseEvent.parseJSON(arrayevents.getJSONObject(i)));
			}
			return events;		 
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static ArrayList<BaseEvent> getEventsBySearch(Integer ownerid, Integer type, String name, String category, ArrayList<String> tags) throws IllegalStateException, IOException, JSONException, RequestException, ParseException {
		List<NameValuePair> l = new ArrayList<NameValuePair>();
		if (name != null)
			l.add(new BasicNameValuePair("name",name));
		if (category != null)
			l.add(new BasicNameValuePair("category",category));
		if (ownerid != null)
			l.add(new BasicNameValuePair("owner_id",ownerid.toString()));
		if (tags != null && !tags.isEmpty()) {
			JSONArray arraytags = new JSONArray(tags);
			JSONObject objtags = new JSONObject();
			objtags.put("tags", arraytags);
			l.add(new BasicNameValuePair("tags",objtags.toString()));
		}
		if (type != null) {
			int typ = type.intValue();
			if (typ == BaseEvent.PUBLIC)
				l.add(new BasicNameValuePair("type","public"));
			else if (typ == BaseEvent.PRIVATE)
				l.add(new BasicNameValuePair("type","private"));
			else if (typ == BaseEvent.GEOLOCATED)
				l.add(new BasicNameValuePair("type","geolocated"));
			else
				throw new RequestException("invalid event type");
		}
		JSONObject object = doRequest(API_EVENTS_URL,GET,l,null);

		String result = object.getString("result");
		if (result.equals("success")) {
			ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
			JSONArray arrayevents = object.getJSONArray("data");
			for (int i = 0 ;i < arrayevents.length(); i++) {
				events.add(BaseEvent.parseJSON(arrayevents.getJSONObject(i)));
			}
			return events;		 
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static ArrayList<Comment> getCommentsAfterDate(int eventid, Date data) throws JSONException, ParseException, RequestException, InvalidParameterException, IllegalStateException, IOException {
		JSONObject object = doRequest(API_COMMENTS_URL+"?eventid="+Integer.toString(eventid)+"&created_at="+DateParser.parseDate(data),GET,null,null);

		String result = object.getString("result");
		if (result.equals("success")) {
			ArrayList<Comment> comments = new ArrayList<Comment>();
			JSONArray arraycomments = object.getJSONArray("data");
			for (int i = 0 ;i < arraycomments.length(); i++) {
				comments.add(Comment.parseJSON(arraycomments.getJSONObject(i)));
			}
			return comments;
		}
		else
			throw new RequestException(object.getString("data"));
	}
	
	public static ArrayList<BaseEvent> filterEvents(ArrayList<BaseEvent> events, int user_id, GPSCoords coords) {
		ArrayList<BaseEvent> returnEvents = new ArrayList<BaseEvent>();
		for (int i = 0 ; i < events.size(); i++) {
			BaseEvent event = events.get(i);
			if (event.getType() == BaseEvent.PRIVATE && event.getAuthorId() != user_id)
				continue;
//			else if (event.getType() == BaseEvent.GEOLOCATED && !inRadius(coords,event.getLocation().getGps()))
//				continue;
			else
				returnEvents.add(event);
		}
		return returnEvents;
	}

	private static boolean inRadius(GPSCoords coords, GPSCoords gps) {
		float[] vals = new float[1];
		android.location.Location.distanceBetween(coords.getLatitude(), coords.getLatitude(), gps.getLatitude(), gps.getLongitude(), vals);
		if (vals[0] <= radius)
			return true;
		else
			return false;
	}
	
}
