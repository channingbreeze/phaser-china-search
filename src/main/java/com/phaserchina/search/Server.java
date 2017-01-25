package com.phaserchina.search;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.phaserchina.search.core.ItemSearchEngine;
import com.phaserchina.search.core.SearchEngine;
import com.phaserchina.search.object.Item;

public class Server {

	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	private SearchEngine searchEngine = new ItemSearchEngine();
	
	public void start(int port) {
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new HttpRequestDecoder());
				pipeline.addLast("aggregator", new HttpChunkAggregator(
						64 * 1000000));
				pipeline.addLast("encoder", new HttpResponseEncoder());
				pipeline.addLast("handler", new SearchApiHandler());
				return pipeline;
			}
		});
		bootstrap.bind(new InetSocketAddress(port));
		System.out.println("WebServer started on " + port);
	}

	class SearchApiHandler extends SimpleChannelUpstreamHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			HttpRequest request = (HttpRequest) e.getMessage();
			String uri = request.getUri();
			String jsonOut = "{success: \"false\"}";

			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
					request.getUri());
			Map<String, List<String>> params = queryStringDecoder
					.getParameters();
			if (uri.startsWith("/search_docs")) {
				List<String> keywords = params.get("keyword");
				if (keywords != null && keywords.size() == 1) {
					logger.info("Search for docs with keyword : "
							+ keywords.get(0));
					List<Item> items = searchEngine.Search(keywords.get(0));
					jsonOut = JSON.toJSONString(items);
					System.out.println(jsonOut);
				}
			}

			HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
			ChannelBuffer buffer = new DynamicChannelBuffer(2048);
			buffer.writeBytes(jsonOut.getBytes("UTF-8"));
			response.setContent(buffer);
			response.setHeader("Content-Type",
					"application/json; charset=UTF-8");
			response.setHeader("Content-Length", response.getContent()
					.writerIndex());
			Channel ch = e.getChannel();
			ch.write(response);
			ch.disconnect();
			ch.close();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			Throwable cause = e.getCause();
			cause.printStackTrace();
			if (cause instanceof TooLongFrameException) {
				sendError(ctx.getChannel(), BAD_REQUEST);
			}
		}

		private void sendError(Channel chan, HttpResponseStatus status) {
			HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
			response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
			response.setContent(ChannelBuffers.copiedBuffer("Failure: "
					+ status.toString() + "\r\n", CharsetUtil.UTF_8));
			chan.write(response).addListener(ChannelFutureListener.CLOSE);
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.start(8000);

	}

}
