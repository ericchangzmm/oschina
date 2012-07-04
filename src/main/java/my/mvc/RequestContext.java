package my.mvc;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.SqlDateConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestContext {

	private final static Log log = LogFactory.getLog(RequestContext.class);

	private final static int MAX_FILE_SIZE = 10 * 1024 * 1024;
	private final static String UTF_8 = "UTF-8";

	private final static ThreadLocal<RequestContext> contexts = new ThreadLocal<RequestContext>();

	private final static String upload_tmp_path;
	private final static String TEMP_UPLOAD_PATH_ATTR_NAME = "$OSCHINA_TEMP_UPLOAD_PATH$";

	public static String webroot = null;

	private ServletContext context;
	private HttpSession session;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private Map<String, Cookie> cookies;

	static {
		webroot = getWebrootPath();

		// 上传的临时目录
		upload_tmp_path = webroot + "WEB-INF" + File.separator + "tmp"
				+ File.separator;
		try {
			FileUtils.forceMkdir(new File(upload_tmp_path));
		} catch (IOException excp) {
		}

		// BeanUtils对时间转换的初始化设置
		ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
		ConvertUtils.register(new Converter() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d");
			SimpleDateFormat sdf_time = new SimpleDateFormat("yyyy-M-d H:m");

			@Override
			public Object convert(Class type, Object value) {
				if (value == null)
					return null;
				if (value instanceof Date)
					return value;
				try {
					return sdf_time.parse(value.toString());
				} catch (ParseException e) {
					try {
						return sdf.parse(value.toString());
					} catch (ParseException e1) {
						return null;
					}
				}
			}
		}, java.util.Date.class);
	}

	private final static String getWebrootPath() {
		String root = RequestContext.class.getResource("/").getFile();
		try {
			root = new File(root).getParentFile().getParentFile()
					.getCanonicalPath();
			root += File.separator;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return root;
	}

	/**
	 * 初始化请求上下文
	 * 
	 * @param ctx
	 * @param req
	 * @param res
	 * @return
	 */
	public static RequestContext begin(ServletContext ctx,
			HttpServletRequest req, HttpServletResponse res) {
		RequestContext rc = new RequestContext();
		rc.context = ctx;
		rc.request = _AutoUploadRequest(_AutoEncodingRequest(req));
		rc.response = res;
		rc.response.setCharacterEncoding(UTF_8);
		rc.session = req.getSession(false);
		rc.cookies = new HashMap<String, Cookie>();
		Cookie[] cookies = req.getCookies();
		if (cookies != null)
			for (Cookie ck : cookies)
				rc.cookies.put(ck.getName(), ck);

		contexts.set(rc);
		return rc;
	}

	/**
	 * 返回Web应用的路径
	 * 
	 * @return
	 */
	public static String root() {
		return webroot;
	}

	/**
	 * 获取当前请求的上下文
	 * 
	 * @return
	 */
	public static RequestContext get() {
		return contexts.get();
	}

	public void end() {
		String tmpPath = (String) request
				.getAttribute(TEMP_UPLOAD_PATH_ATTR_NAME);
		if (tmpPath != null) {
			try {
				FileUtils.deleteDirectory(new File(tmpPath));
			} catch (IOException e) {
				log.fatal("Failed to cleanup upload directory: " + tmpPath, e);
			}
		}
		this.context = null;
		this.request = null;
		this.response = null;
		this.session = null;
		this.cookies = null;
		contexts.remove();
	}

	public Locale locale() {
		return request.getLocale();
	}

	public void closeCache() {
		header("Pragma", "No-cache");
		header("Cache-Control", "no-cache");
		header("Expires", 0L);
	}
	
	/**
	 * 自动编码处理
	 * @param req
	 * @return
	 */
	private static HttpServletRequest _AutoEncodingRequest(HttpServletRequest req) {
		if(req instanceof RequestProxy)
			            return req;

	}

	
	public String header(String name){
		return request.getHeader(name);
	}
	
	public void header(String name, String value){
		response.setHeader(name, value);
	}
	
	public void header(String name, int value){
		response.setIntHeader(name, value);
	}
	
	public void header(String name, long value){
		response.setDateHeader(name, value);
	}

	/**
	 * 自动解码
	 * @author eric.zhang
	 */
	private static class RequestProxy extends HttpServletRequestWrapper {
		
	}

}
