/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2016, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package thymeleafexamples.stsm.web.controller;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import thymeleafexamples.stsm.business.models.Quote;
import thymeleafexamples.stsm.business.models.User;
import thymeleafexamples.stsm.business.models.Value;
import thymeleafexamples.stsm.business.services.GitHubLookupService;


@Controller
public class SeedStarterMngController {
    final Log logger = LogFactory.getLog(getClass());
    RestTemplate restTemplate = new RestTemplate();
    AsyncRestTemplate asyncTemplate = new AsyncRestTemplate(new HttpComponentsAsyncClientHttpRequestFactory());

    @Autowired
    private GitHubLookupService gitHubLookupService;

    public SeedStarterMngController() {
        super();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    private Quote makeRESTReqForQuoteAsyncRT() {

        ListenableFuture<ResponseEntity<Quote>> quoteFuture = this.asyncTemplate.getForEntity("https://gturnquist-quoters.cfapps.io/api/random", Quote.class);
        Quote retQuote = new Quote();
        try {
            ResponseEntity<Quote> res = quoteFuture.get();
            retQuote = res.getBody();
        } catch (Exception e) {
            retQuote.setType("Error");
            Value errVal = new Value();
            errVal.setId(-1L);
            errVal.setQuote("Exception: " + e.getMessage());
            retQuote.setValue(errVal);
        }
        return retQuote;
    }

    private Quote makeRESTReqForQuoteSyncRT() {
        Quote quote = this.restTemplate.getForObject("https://gturnquist-quoters.cfapps.io/api/random", Quote.class);
        return quote;
    }

    private String makeRESTReqForGithubSpringAsync() {
        String res = "";
        try {
            CompletableFuture<User> page1 = gitHubLookupService.findUser("PivotalSoftware");
            CompletableFuture<User> page2 = gitHubLookupService.findUser("CloudFoundry");
            CompletableFuture<User> page3 = gitHubLookupService.findUser("Spring-Projects");

            // Wait until they are all done
            CompletableFuture.allOf(page1,page2,page3).join();

            res += page1.get().toString() + "\n" + page2.get().toString() + "\n" + page3.get().toString();
        } catch (Exception e) {
            res = e.getMessage();
        }

        return res;
    }

    private String makeRESTPOSTReq() {
        MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        bodyMap.add("id", "2");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(bodyMap, headers);

        String returnString = "POST WILL FAIL MOST LIKELY DUE TO LOCALHOST SERVER NOT RUNNING";
        try {
            ResponseEntity<String> ret = this.restTemplate.postForEntity("http://localhost:9001/uppercase", request, String.class);
            returnString = ret.getBody();
        } catch (Exception e) {
            returnString = e.getMessage();
        }

        return returnString;
    }

    @RequestMapping({"/resttemplatetest"})
    public ModelAndView handleRequest() {
        Quote quote = makeRESTReqForQuoteSyncRT();
        Quote asyncQuote = makeRESTReqForQuoteAsyncRT();
        logger.info("Return View: " + quote);
        ModelAndView mav = new ModelAndView("resttemplatetest.html");
        mav.addObject("springasync", makeRESTReqForGithubSpringAsync());
        mav.addObject("postres", makeRESTPOSTReq());
        mav.addObject("syncquotetype", quote.getType());
        mav.addObject("syncquoteval", quote.getValue().getQuote());
        mav.addObject("asyncquotetype", asyncQuote.getType());
        mav.addObject("asyncquoteval", asyncQuote.getValue().getQuote());
        return mav;
    }

    @RequestMapping({"/asyncresttemplatetest"})
    public ModelAndView handleRequestAsyncTemplate() {
        Quote asyncQuote = makeRESTReqForQuoteAsyncRT();
        logger.info("Return View: " + asyncQuote);
        ModelAndView mav = new ModelAndView("resttemplatetest.html");
        mav.addObject("asyncquotetype", asyncQuote.getType());
        mav.addObject("asyncquoteval", asyncQuote.getValue().getQuote());
        return mav;
    }

}
