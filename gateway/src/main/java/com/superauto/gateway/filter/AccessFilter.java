package com.superauto.gateway.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.dubbo.common.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.superauto.gateway.config.RetResult;
import com.superauto.gateway.utils.DubboCallbackUtil;
import com.superauto.gateway.vo.DubboRouteVO;


@Component
public class AccessFilter extends ZuulFilter {
 
	@Value("${dubbo.registry.address}")
    private String zookeeperAddress;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private Map<String, Object> params;
    private static Logger log = LoggerFactory.getLogger(AccessFilter.class);
    @Override
    public String filterType() {
        //前置过滤器
        return "pre";
    }
 
    @Override
    public int filterOrder() {
        //优先级，数字越大，优先级越低
        return 0;
    }
 
    @Override
    public boolean shouldFilter() {
        //是否执行该过滤器，true代表需要过滤
        return true;
    }
 
    @Override
    public Object run() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setSendZuulResponse(false);//对请求不进行http路由转发
        ctx.getResponse().setContentType("application/json; charset=utf-8");
        String value = "";
        HttpServletRequest req = (HttpServletRequest)RequestContext.getCurrentContext().getRequest();
        System.err.println("REQUEST:: " + req.getScheme() + " " + req.getRemoteAddr() + ":" + req.getRemotePort());
        String pathurl =  req.getRequestURI();
        BufferedReader reader = null;
        String body = null;
        RetResult retResult = new RetResult();
        Map<String,Object> params = new HashMap<>();
		try {
			reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
			body = IOUtils.read(reader).replaceAll("\t|\n|\r", "");
			params = JSON.parseObject(body);
		}catch (IOException e) {
			retResult.setCode(401);
			retResult.setMsg("数据格式有问题!");
			ctx.setResponseBody(JSON.toJSONString(retResult));
            log.error("流读取错误："+e);
            return null;
        }catch(JSONException e)
		{
        	Map<String, String[]> parameterMap = req.getParameterMap();
	        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
	             for (String string : entry.getValue()) {
	            	 value = string;
	             }
	            params.put(entry.getKey(), value);
	        }
		}finally {
            if (null != reader){
                try {
                    reader.close();
                } catch (IOException e) {
                	retResult.setCode(401);
        			retResult.setMsg("流关闭有问题!");
        			ctx.setResponseBody(JSON.toJSONString(retResult));
                    log.error("流关闭错误："+e);
                    return null;
                }
            }
        }
        String paramsJson = JSON.toJSONString(params);
		String sql = "select * from gateway_dubboapi_define where urlPath = ?";
		List<DubboRouteVO>  dubboVoList  = jdbcTemplate.query(sql, new Object[] {pathurl},new BeanPropertyRowMapper<>(DubboRouteVO.class));
        if(dubboVoList.size()==0) {
           log.warn("url is not exist");
           retResult.setCode(401);
		   retResult.setMsg("请求的url是不存在的!");
		   ctx.setResponseBody(JSON.toJSONString(retResult));
		   return null;
        }
        String interfaceName = dubboVoList.get(0).getInterfaceName();
        String methodName = dubboVoList.get(0).getMethodName();
        String version = dubboVoList.get(0).getVersion();
        System.out.println("接口:" + interfaceName);
        System.out.println("方法:" + methodName);
        System.out.println("版本号:" + version);
        Object dubboObject =  DubboCallbackUtil.invoke(interfaceName, methodName ,paramsJson, zookeeperAddress, version);
        retResult.setCode(200);
        retResult.setData(dubboObject);
        ctx.setResponseBody(JSON.toJSONString(retResult));
        return null;
    }
}
