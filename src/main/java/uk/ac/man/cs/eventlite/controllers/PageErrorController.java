package uk.ac.man.cs.eventlite.controllers;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class PageErrorController implements ErrorController  {

    @RequestMapping("/error")
    public String handleHttpError(HttpServletRequest webrequest) {
        int status = (Integer) webrequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        switch (status) {
          case 400: return "error/400";
          case 401: return "error/401";
          case 404: return "error/404";
          case 500: return "error/500";
        }
        return "error/general";
    }
}
