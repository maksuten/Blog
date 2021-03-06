package pl.upir.blog.web.restful.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.upir.blog.entity.BlgUser;
import pl.upir.blog.service.BlgUserService;
import pl.upir.blog.wrapper.WrapperUserDetailJson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Vitalii on 09.07.2015.
 */
@Controller
@RequestMapping("/rest")
public class BlgUserApi {

    @Autowired
    BlgUserService blgUserService;

    @RequestMapping("/1")
    public @ResponseBody String getStr(){
        return "fuck";
    }

    @RequestMapping(value = "/test",method = RequestMethod.GET)
    public @ResponseBody
    WrapperUserDetailJson getUser(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usrLogin = ((BlgUser) principal).getUsrLogin();
        BlgUser blgUser = blgUserService.findByUsrLogin(usrLogin);
       // httpServletResponse.addHeader("ContentType","application/json");
        WrapperUserDetailJson wrapperUserDetailJson = new WrapperUserDetailJson();
        wrapperUserDetailJson.setLogin(blgUser.getUsrLogin());
        wrapperUserDetailJson.setFirstname(blgUser.getBlgUserDetail().getUsrDetFirstname());
        wrapperUserDetailJson.setLastname(blgUser.getBlgUserDetail().getUsrDetLastname());

        return wrapperUserDetailJson;
    }
}
