package com.netease.pomelo;

import io.socket.SocketIOException;
import io.socket2.IOAcknowledge;
import io.socket2.IOCallback;
import io.socket2.IOMessage;
import io.socket2.SocketIO;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class PomeloClient2 {

	private static final Logger logger = Logger.getLogger("org.netease.pomelo");
	private final static String URLHEADER = "http://";
	private final static String JSONARRAY_FLAG = "[";
	private int reqId;
	private SocketIO socket;
	private Map<String, DataCallBack> cbs;
	private Map<String, List<DataListener>> listeners;

	public PomeloClient2(String url, int port) {
		initSocket(url, port);
		cbs = new HashMap<String, DataCallBack>();
		listeners = new HashMap<String, List<DataListener>>();
	}

	/**
	 * Init the socket of pomelo client.
	 * 
	 * @param url
	 * @param port
	 */
	private void initSocket(String url, int port) {
		StringBuffer buff = new StringBuffer();
		if (!url.contains(URLHEADER))
			buff.append(URLHEADER);
		buff.append(url);
		buff.append(":");
		buff.append(port);
		socket = new SocketIO(buff.toString());
		socket.setFactory(new PomeloFactory());
		//socket.setCallback(new IOCallback() {
		//	@Override
		//	public void onDisconnect() {
		//		logger.warning("------------onDisconnect");
		//	}
        //
		//	@Override
		//	public void onConnect() {
		//		logger.warning("------------onConnect");
		//	}
        //
		//	@Override
		//	public void onConnectFailed() {
		//		logger.warning("------------onConnectFailed");
		//	}
        //
		//	@Override
		//	public void onMessage(IOMessage message, IOAcknowledge ack) {
		//		processMessage((PomeloMessage) message);
		//	}
        //
		//	@Override
		//	public void onMessage(List<IOMessage> messages, IOAcknowledge ack) {
		//		processMessageBatch(messages);
		//	}
        //
		//	@Override
		//	public void onMessageFailed(IOMessage message) {
        //
		//	}
        //
		//	@Override
		//	public void on(String event, IOAcknowledge ack, Object... args) {
        //
		//	}
        //
		//	@Override
		//	public void onError(SocketIOException socketIOException) {
		//		logger.warning("------------onError");
		//	}
		//});
	}

	public void connect()  {
		socket.connect();
	}

	/**
	 * Send message to the server side.
	 * 
	 * @param reqId
	 *            request id
	 * @param route
	 *            request route
	 * @param msg
	 *            reqest message
	 */
	private void sendMessage(int reqId, String route, String msg) {
		socket.sendIOMessage(new PomeloMessage(reqId, route, msg));
	}

	/**
	 * Client send request to the server and get response data.
	 * 
	 * @param args
	 */
	public void request(Object... args) {
		if (args.length < 2 || args.length > 3) {
			throw new RuntimeException("the request arguments is error.");
		}
		// first argument must be string
		if (!(args[0] instanceof String)) {
			throw new RuntimeException("the route of request is error.");
		}

		String route = args[0].toString();
		JSONObject msg = null;
		DataCallBack cb = null;

		if (args.length == 2) {
			if (args[1] instanceof JSONObject)
				msg = (JSONObject) args[1];
			else if (args[1] instanceof DataCallBack)
				cb = (DataCallBack) args[1];
		} else {
			msg = (JSONObject) args[1];
			cb = (DataCallBack) args[2];
		}
		msg = filter(msg);
		reqId++;
		cbs.put(String.valueOf(reqId), cb);
		sendMessage(reqId, route, msg.toString());
	}

	/**
	 * Notify the server without response
	 * 
	 * @param route
	 * @param msg
	 */
	public void notify(String route, JSONObject msg) {
		request(route, msg);
	}

	/**
	 * Add timestamp to message.
	 * 
	 * @param msg
	 * @return msg
	 */
	private JSONObject filter(JSONObject msg) {
		if (msg == null) {
			msg = new JSONObject();
		}
		long date = System.currentTimeMillis();
		try {
			msg.put("timestamp", date);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return msg;
	}

	/**
	 * Disconnect the connection with the server.
	 */
	public void disconnect() {
		socket.disconnect();
	}

	/**
	 * Process the message from the server.
	 * 
	 * @param msg
	 */
	private void processMessage(PomeloMessage msg) {
		if (msg.getReqId() != null) {
			try {
				DataCallBack cb = cbs.get(String.valueOf(msg.getReqId()));
				cb.responseData(new JSONObject(msg.getText()));
				cbs.remove(msg.getReqId());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			emit(msg.getRoute(), new JSONObject());
		}
	}

	/**
	 * Process message in batch.
	 *
	 * @param msgs
	 */
	private void processMessageBatch(List<IOMessage> msgs) {
		for (IOMessage message : msgs) {
			processMessage((PomeloMessage) message);
		}
	}

	/**
	 * Add event listener and wait for broadcast message.
	 * 
	 * @param route
	 * @param listener
	 */
	public void on(String route, DataListener listener) {
		List<DataListener> list = listeners.get(route);
		if (list == null)
			list = new ArrayList<DataListener>();
		list.add(listener);
		listeners.put(route, list);
	}

	/**
	 * Touch off the event and call listeners corresponding route.
	 * 
	 * @param route
	 * @param message
	 * @return true if call success, false if there is no listeners for this
	 *         route.
	 */
	private void emit(String route, JSONObject message) {
		List<DataListener> list = listeners.get(route);
		if (list == null) {
			logger.warning("there is no listeners.");
			return;
		}
		for (DataListener listener : list) {
			DataEvent event = new DataEvent(this, message);
			listener.receiveData(event);
		}
	}

	//public void reconnect() {
	//	socket.reconnect();
	//}
}
