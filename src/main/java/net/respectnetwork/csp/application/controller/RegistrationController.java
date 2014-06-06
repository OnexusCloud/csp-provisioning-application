package net.respectnetwork.csp.application.controller;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import net.respectnetwork.csp.application.csp.CurrencyCost;
import net.respectnetwork.csp.application.dao.DAOException;
import net.respectnetwork.csp.application.dao.DAOFactory;
import net.respectnetwork.csp.application.exception.UserRegistrationException;
import net.respectnetwork.csp.application.form.PaymentForm;
import net.respectnetwork.csp.application.form.SignUpForm;
import net.respectnetwork.csp.application.form.UserDetailsForm;
import net.respectnetwork.csp.application.form.ValidateForm;
import net.respectnetwork.csp.application.manager.RegistrationManager;
import net.respectnetwork.csp.application.model.CSPCostOverrideModel;
import net.respectnetwork.csp.application.model.CSPModel;
import net.respectnetwork.csp.application.model.InviteModel;
import net.respectnetwork.csp.application.session.RegistrationSession;
import net.respectnetwork.csp.application.util.FormErrorsHelper;
import net.respectnetwork.sdk.csp.validation.CSPValidationException;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import xdi2.core.xri3.CloudNumber;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Handles requests for the application home page.
 */
@Controller
public class RegistrationController
{

   /** Class Logger */
   private static final Logger logger = LoggerFactory
                                            .getLogger(RegistrationController.class);

   /** Registration Manager */
   private RegistrationManager theManager;

   /** Registration Session */
   private RegistrationSession regSession;

   private String              cspCloudName;
   
   public static final String URL_PARAM_NAME_REQ_CLOUDNAME     = "name"   ;

   private static LookupService        geoIpLookupService = null;

   @Autowired
   private MessageSource messageSource;

   private static synchronized void geoIpLookupServiceInit()
   {
           if( geoIpLookupService != null )
           {
                   return;
           }
           URL fileResource = RegistrationController.class.getClassLoader()
                 .getResource("GeoLiteCity.dat"); 
           if(fileResource != null)
           {
              String fileName = fileResource.getFile() ; 
              logger.info("GeoIpLookupServiceInit - " + fileName);
              try
              {
                      geoIpLookupService = new LookupService(fileName, LookupService.GEOIP_MEMORY_CACHE);
                      logger.info("GeoIpLookupServiceInit - " + fileName + " Done " + geoIpLookupService);
              }
              catch( java.io.IOException e )
              {
                      logger.error("Cannot initialize GeoIpLookupService - " + fileName, e);
              }
           } else
           {
              logger.error("Cannot initialize GeoIpLookupService - " + "GeoLiteCity.dat");
           }
           
   }

   public String getCspCloudName()
   {
      return this.cspCloudName;
   }

   @Autowired
   @Qualifier("cspCloudName")
   public void setCspCloudName(String cspCloudName)
   {
      this.cspCloudName = cspCloudName;
   }

   /**
    * 
    * @return
    */
   public RegistrationManager getTheManager()
   {
      return theManager;
   }

   /**
    * 
    * @param theManager
    */
   @Autowired
   @Qualifier("active")
   @Required
   public void setTheManager(RegistrationManager theManager)
   {
      this.theManager = theManager;
   }

   /**
    * @return the regSession
    */
   public RegistrationSession getRegSession()
   {
      return regSession;
   }

   /**
    * @param regSession
    *           the regSession to set
    */
   @Autowired
   public void setRegSession(RegistrationSession regSession)
   {
      this.regSession = regSession;
   }
    /**
     * Flag to check if referer required or not to register
     */
    @Value("${csp.refererRequired}")
    private String refererRequired;
    /**
     * Referer URL
     */
    @Value("${csp.refererURL}")
    private String refererURL;

    /**
     * Initial Sign-Up Page
     */
    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public ModelAndView signup(@Valid @ModelAttribute("signUpInfo") SignUpForm signUpForm,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               BindingResult result) {
        logger.debug("Starting the Sign Up Process");

        FormErrorsHelper errors = new FormErrorsHelper(messageSource, request);

        String cloudName = signUpForm.getCloudName();
        String inviteCode = signUpForm.getInviteCode();
        String giftCode = signUpForm.getGiftCode();
        logger.debug("cloudName={}, inviteCode={}, giftCode={}", cloudName, inviteCode, giftCode);

        // Validate invite code
        if (isNullOrEmpty(inviteCode)) {
            if (theManager.isRequireInviteCode()) {
                logger.debug("Invite code required but missing :: inviteCode={}", inviteCode);
                errors.add("error", "signUp.msg.invite.codeMissing");
            }
        } else {
            try {
                DAOFactory dao = DAOFactory.getInstance();
                InviteModel invite = dao.getInviteDAO().get(inviteCode);
                String invitedEmailAddr = invite.getInvitedEmailAddress();

                if (!isNullOrEmpty(invitedEmailAddr)) {
                    // todo save invite code somewhere
                    // userDetailsForm.setEmail(invite.getInvitedEmailAddress()); // can't put it in form as it can be manipulated very easily
                    logger.error("todo invitee code save not implemented!");
                } else {
                    logger.warn("Invite code not associated with user :: inviteCode={}", inviteCode);
                    errors.add("error", "signUp.msg.invite.invalid");
                }
            } catch (Exception e) {
                logger.error("Error validating invite code", e);
                errors.add("error", "signUp.msg.invite.validationError");
            }
        }

        // Validate cloud name
        if (errors.isEmpty()) {
            if (isNullOrEmpty(cloudName)) {
                logger.debug("No cloud name supplied :: cloudName={}", cloudName);
                errors.add("error", "signUp.msg.invalid");
            } else {
                if (!cloudName.startsWith("=")) {
                    cloudName = "=" + cloudName;
                }

                if (!RegistrationManager.validateCloudName(cloudName)) {
                    logger.debug("Cloud name invalid :: cloudName={}", cloudName);
                    errors.add("error", "signUp.msg.invalid");
                } else {
                    try {
                        if (!theManager.isCloudNameAvailable(cloudName)) {
                            logger.debug("Cloud name not available :: cloudName={}", cloudName);
                            errors.add("error", "signUp.msg.unavailable");
                        }
                    } catch (UserRegistrationException e) {
                        logger.error("Error checking if cloud name available");
                        errors.add("error", "signUp.msg.nameCheckError");
                    }
                }
            }
        }

        // Response
        ModelAndView mv;
        if (errors.isEmpty()) {
            mv = new ModelAndView("userdetails");
            mv.addObject("userInfo", new UserDetailsForm());

            regSession.setSessionId(UUID.randomUUID().toString());
            regSession.setCloudName(cloudName);
            regSession.setGiftCode(giftCode);
        } else {
            mv = new ModelAndView("signUp");
            mv.addObject("signUpInfo", signUpForm);
        }

        mv.addObject("cloudName", cloudName);
        return errors.withModelView(mv);
    }

    @ResponseBody
    @RequestMapping(value="/checkCloudName", method = RequestMethod.GET, params = {"cloudName", "callback"})
    public String checkCloudName(@RequestParam("cloudName") String cloudName,
                                 @RequestParam("callback") String callback) {
        boolean error = false;
        boolean valid = false;
        boolean available = false;

        try {
            if (!isNullOrEmpty(cloudName) && !cloudName.contains(" ")) {
                if (!cloudName.startsWith("=")) {
                    cloudName = "=" + cloudName;
                }

                valid = RegistrationManager.validateCloudName(cloudName);
                if (valid) {
                    available = theManager.isCloudNameAvailable(cloudName);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to check cloud name", e);
            error = true;
        }

        return String.format("%s({'isError':%s, 'isValid':%s, 'isAvailable':%s});", callback, error, valid, available);
    }

    /**
     * Get User Details
     */
    @RequestMapping(value = "/processuserdetails", method = RequestMethod.POST)
    public ModelAndView getDetails(@Valid @ModelAttribute("userInfo") UserDetailsForm userDetailsForm,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   BindingResult result) {
        logger.debug("Get User Details");

        FormErrorsHelper errors = new FormErrorsHelper(messageSource, request);

        // Validate session
        String sessionId = regSession.getSessionId();
        String cloudName = regSession.getCloudName();
        if (isNullOrEmpty(sessionId) || isNullOrEmpty(cloudName)) {
            logger.debug("Invalid sessionId or cloudName :: sessionId={}, cloudName={}", sessionId, cloudName);
            errors.add("error", "form.invalidSession");

            ModelAndView mv = new ModelAndView("userdetails");
            mv.addObject("userInfo", userDetailsForm);
            mv.addObject("cloudName", cloudName);
            return  errors.withModelView(mv);
        }

        // Validate email
        String email = userDetailsForm.getEmail();
        if (!EmailValidator.getInstance().isValid(email)) {
            logger.debug("Invalid email :: email={}", email);
            errors.add("email", "userDetails.msg.email.invalid");
        }

        // Validate phone
        String phone = userDetailsForm.getPhone();
        if (!RegistrationManager.validatePhoneNumber(phone)) {
            logger.debug("Invalid phone :: phone={}", phone);
            errors.add("mobilePhone", "userDetails.msg.phone.invalid");
        }

        // Validate password
        String password = userDetailsForm.getPassword();
        if (!RegistrationManager.validatePassword(password)) {
            logger.debug("Invalid password :: password={}" + password);
            errors.add("password", "userDetails.msg.password.invalid");
        }

        // Validate confirm password
        String confirmPassword = userDetailsForm.getConfirmPassword();
        if (isNullOrEmpty(password) || !password.equals(confirmPassword)) {
            logger.debug("password != confirmPassword :: password={}, confirmPassword={}", password, confirmPassword);
            errors.add("confirmPassword", "userDetails.msg.confirmPassword.equalTo");
        }

        // Validate existing user
        try {
            CloudNumber[] existingUsers = theManager.checkEmailAndMobilePhoneUniqueness(phone, email);
            if (existingUsers[0] != null) {
                logger.debug("Phone not unique :: phone={}, existingUser={}", phone, existingUsers[0]);
                errors.add("mobilePhone", "userDetails.msg.phone.used");
            }
            if (existingUsers[1] != null) {
                logger.debug("Email not unique :: email={}, existingUser={}", email, existingUsers[1]);
                errors.add("email", "userDetails.msg.email.used");
            }
        } catch (Exception e) {
            logger.error("Failed to check email and phone uniqueness!", e);
            errors.add("error", "userDetails.msg.uniquenessError");
        }

        // Send validation codes
        if (errors.isEmpty()) {
            try {
                theManager.sendValidationCodes(sessionId, email, phone);
            } catch (Exception e) {
                logger.warn("Failed to send validation codes", e);
                errors.add("error", "userDetails.msg.sendCodesError");
            }
        }

        // Response
        ModelAndView mv;
        if (errors.isEmpty()) {
            mv = new ModelAndView("validate");
            mv.addObject("validateInfo", new ValidateForm());
            mv.addObject("verifyingEmail", email);
            mv.addObject("verifyingPhone", phone);

            // Set email, phone, and password to session
            logger.debug("Setting verified email {}", email);
            regSession.setVerifiedEmail(email);
            regSession.setVerifiedMobilePhone(phone);
            regSession.setPassword(password);
        } else {
            mv = new ModelAndView("userdetails");
            mv.addObject("userInfo", userDetailsForm);
        }

        mv.addObject("cloudName", cloudName);
        return  errors.withModelView(mv);
    }

    /**
     * Validate Confirmation Codes
     */
    @RequestMapping(value = "/validatecodes", method = RequestMethod.POST)
    public ModelAndView validateCodes(@Valid @ModelAttribute("validateInfo") ValidateForm validateForm,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      BindingResult result) {
        logger.debug("Starting Validation Process");
        logger.debug("Processing Validation Data: {}", validateForm.toString());

        FormErrorsHelper errors = new FormErrorsHelper(messageSource, request);

        ModelAndView mv = new ModelAndView("validate");
        mv.addObject("validateInfo", validateForm);
        mv.addObject("verifyingEmail", regSession.getVerifiedEmail());
        mv.addObject("verifyingPhone", regSession.getVerifiedMobilePhone());
        mv.addObject("cloudName", regSession.getCloudName());

        // Validate session
        String sessionId = regSession.getSessionId();
        String cloudName = regSession.getCloudName();
        if (isNullOrEmpty(sessionId) || isNullOrEmpty(cloudName)) {
            logger.debug("Invalid sessionId or cloudName :: sessionId={}, cloudName={}", sessionId, cloudName);
            errors.add("error", "form.invalidSession");

            return errors.withModelView(mv);
        }

        // Resending codes
        if (request.getParameter("resendCodes") != null) {
            try {
                theManager.sendValidationCodes(sessionId, regSession.getVerifiedEmail(), regSession.getVerifiedMobilePhone());
            } catch (CSPValidationException e) {
                logger.warn("Failed to send validation codes", e);
                errors.add("error", "userDetails.msg.sendCodesError");
            }

            return errors.withModelView(mv);
        }

        // Validate Codes
        Locale locale = request.getLocale();
        String emailCode = validateForm.getEmailCode();
        String smsCode = validateForm.getSmsCode();
        emailCode = (emailCode != null) ? emailCode.trim().toUpperCase(locale) : null;
        smsCode = (smsCode != null) ? smsCode.trim().toUpperCase(locale) : null;
        if (!theManager.validateCodes(sessionId, emailCode, smsCode)) {
            logger.debug("Code validation failed :: emailCode={}, smsCode={}", emailCode, smsCode);
            errors.add("error", "validateCodes.msg.validationFailed");
        }

        // Validate terms
        if(!validateForm.isTermsChecked()) {
            logger.debug("Respect Trust Framework not checked");
            errors.add("terms", "validateCodes.msg.terms.required");
        }

        // Response
        if (errors.isEmpty()) {
            if (validateForm.isResetPwd()) {
                mv = new ModelAndView("resetPassword");
            } else {
                CSPModel cspModel = null;
                String cspCloudName = getCspCloudName();
                try {
                    cspModel = DAOFactory.getInstance().getCSPDAO().get(cspCloudName);
                    if (cspModel == null) {
                        errors.add("error", "form.databaseError");
                    }
                } catch (Exception e) {
                    logger.error("Failed to get cspModel :: cspCloudName={}", cspCloudName);
                    errors.add("error", "form.databaseError");
                }

                if (errors.isEmpty() && cspModel != null) {
                    PaymentForm paymentForm = new PaymentForm();
                    paymentForm.setNumberOfClouds(1);
                    paymentForm.setTxnType(PaymentForm.TXN_TYPE_SIGNUP);

                    // Gift codes
                    String giftCode = regSession.getGiftCode();
                    if (!isNullOrEmpty(giftCode)) {
                        logger.debug("Setting gift code from session :: giftCode={}", giftCode);
                        paymentForm.setGiftCodes(regSession.getGiftCode());
                    }
                    if ("GIFT_CODE_ONLY".equals(cspModel.getPaymentGatewayName())) {
                        paymentForm.setGiftCodesOnly(true);
                    }

                    // Cost override
                    CurrencyCost totalCost = getCostIncludingOverride(cspModel, regSession.getVerifiedMobilePhone(), paymentForm.getNumberOfClouds());

                    // Update session
                    regSession.setCurrency(totalCost.getCurrencyCode());
                    regSession.setCostPerCloudName(totalCost.getAmount());
                    regSession.setTransactionType(paymentForm.getTxnType());

                    mv = new ModelAndView("payment");
                    mv.addObject("paymentInfo", paymentForm);
                    mv.addObject("totalAmountText", formatCurrencyAmount(totalCost));
                }
            }
        }

        mv.addObject("cloudName", regSession.getCloudName());
        return errors.withModelView(mv);
    }

    /**
     * Calculate the cost of buying cloud names, taking cost overrides into account
     */
    static CurrencyCost getCostIncludingOverride(CSPModel cspModel, String phoneNumber, int numberOfClouds) {
        String currency = cspModel.getCurrency();
        BigDecimal costPerCloud = cspModel.getCostPerCloudName();

        CSPCostOverrideModel cspCostOverrideModel;
        try {
            cspCostOverrideModel = DAOFactory.getInstance().getcSPCostOverrideDAO().get(cspModel.getCspCloudName(), phoneNumber);
            if (cspCostOverrideModel != null) {
                logger.debug("Cost override found: " + cspCostOverrideModel.toString());
                currency = cspCostOverrideModel.getCurrency();
                costPerCloud = cspCostOverrideModel.getCostPerCloudName();
            } else {
                logger.debug("No cost override found (using default cost)");
            }
        } catch (DAOException e) {
            logger.error(e.toString());
        }

        CurrencyCost costOneCloud = new CurrencyCost(currency, costPerCloud);
        return costOneCloud.multiply(numberOfClouds);
    }

    /**
     * Format a currency and amount for human display.
     */
    static String formatCurrencyAmount(CurrencyCost currencyCost) {
        return formatCurrencyAmount(currencyCost.getCurrencyCode(), currencyCost.getAmount());
    }

    /**
     * Format a currency and amount for human display.
     */
    static String formatCurrencyAmount(String currency, BigDecimal amount) {
        // Hack - JDK doesn't seem to have an easy locale-independent way to get this symbol
        String currencySymbol = "";
        if (currency.equals("USD") || currency.equals("AUD")) {
            currencySymbol = "$";
        }
        return String.format("%s%04.2f %s", currencySymbol, amount, currency);
    }

   /**
    * This is the endpoint where the user lands in the CSP website from an
    * invite
    * 
    * @param request
    *           : some query parameters that come from RN which are to be echoed
    *           back and a query parameter called "name" which has the cloudname
    * @param response
    * @return
    */
   @RequestMapping(value = "/register", method = RequestMethod.GET)
   public ModelAndView registerCloudName(HttpServletRequest request,
         HttpServletResponse response,
         @Valid @ModelAttribute("signUpInfo") SignUpForm signUpForm,
         BindingResult result)
   {
      boolean errors = false;
      String error = "";
      ModelAndView mv = null;
      String rnQueryString = "";
      //SignUpForm signUpForm = new SignUpForm();
      logger.debug("Referer URL " + request.getHeader("referer"));
      // check for referer URL and if it does not match with the configured
      // one in the
      // properties, then re-direct  to referer URL mentioned in the config file.
      if (Boolean.parseBoolean(refererRequired)
              && (request.getHeader("referer") == null || !request.getHeader(
                      "referer").equals(refererURL))) {
            return new ModelAndView("redirect:"+refererURL);
      }
      Enumeration<String> paramNames = request.getParameterNames(); 
      while(paramNames.hasMoreElements())
      {
         String paramName = paramNames.nextElement();
         logger.debug("p name " + paramName);
         String[] paramValues = request.getParameterValues(paramName);
         for(int i = 0 ; i < paramValues.length ; i++)
         {
            logger.debug("p value " + paramValues[i]);
          //ignore the "name" parameter. Capture rest of it
            if(!paramName.equalsIgnoreCase(URL_PARAM_NAME_REQ_CLOUDNAME))
            {
               try
               {
                  rnQueryString = rnQueryString + "&" + paramName + "=" + URLEncoder.encode(paramValues[i], "UTF-8");
               } catch (UnsupportedEncodingException e)
               {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }
            }
         }
      }
      
      String remoteIPAddr = request.getHeader("X-FORWARDED-FOR");
      
      logger.debug("User agent " + request.getHeader("User-Agent"));

      if(remoteIPAddr == null)
      {
         remoteIPAddr = request.getRemoteAddr();
         
      }
      logger.debug("Client IP " + remoteIPAddr);
      logger.debug("Referer URL " + request.getHeader("referer"));
      //TODO : check for referer URL and if it does not match with the configured one in the 
      //properties, then bail out
      
      logger.info("getLocation - " + remoteIPAddr);

      if( geoIpLookupService == null )
      {
              geoIpLookupServiceInit();
      }
      Location loc = geoIpLookupService.getLocation(remoteIPAddr);
      if( loc == null )
      {
              logger.info("Cannot find location for IP address - " + remoteIPAddr);
              remoteIPAddr = "209.173.53.233";
              loc = geoIpLookupService.getLocation(remoteIPAddr);
      }

      logger.info("getLocation - " + remoteIPAddr + " LAT = " + loc.latitude + " LNG = " + loc.longitude);
      theManager.getEndpointURI(RegistrationManager.GeoLocationPostURIKey, theManager.getCspRegistrar().getCspInformation().getRnCloudNumber());     

      String cloudName = null;
      if (signUpForm != null && signUpForm.getCloudName() != null)
      {
         cloudName = signUpForm.getCloudName();
         try
         {
            cloudName = URLDecoder.decode(cloudName,"UTF-8");
         } catch (UnsupportedEncodingException e)
         {
            logger.debug("Exception for cloudname " + cloudName);
            logger.debug(e.getMessage());
            errors = true;
            error = "Sorry ! The system has encountered an error. Please try again.";
         }
      } else
      {
         cloudName = request.getParameter(URL_PARAM_NAME_REQ_CLOUDNAME);
      }
      
      logger.info("registerCloudName : registration request for cloudname " + cloudName);
      
      if (cloudName != null)
      {
         if(!RegistrationManager.validateCloudName(cloudName))
         {
            errors = true;
            error = RegistrationManager.validINameFormat;
         }
         try
         {
			 //Added one more condition to check registry too using AvailabilityAPI for cloud name. 
            if (theManager.isCloudNameAvailableInRegistry(cloudName) && theManager.isCloudNameAvailable(cloudName))
            {
               logger.info(cloudName + " is available, so going to show the validation screen");
               mv = new ModelAndView("userdetails");
               UserDetailsForm userDetailsForm = new UserDetailsForm();
               mv.addObject("userInfo", userDetailsForm);
               // Add CloudName to Session
               String sessionId = UUID.randomUUID().toString();
               regSession.setSessionId(sessionId);
               regSession.setCloudName(cloudName);
               regSession.setRnQueryString(rnQueryString);
               regSession.setLongitude((long)loc.longitude);
               regSession.setLatitude((long)loc.latitude);
               regSession.setGiftCode(null);;
            } else 
            {
               errors = true;
               error = "CloudName is not available. Please choose another valid CloudName";
            }
         } catch (Exception e) {
            logger.info("Exception in registerCloudName " + e.getMessage());
            errors = true;
            error = "Sorry ! The system has encountered an error. Please try again.";
            // e.printStackTrace();
         }

      } else
      {
         errors = true;
         error = "Please provide a valid cloudname";
      }
      if(errors)
      {
         mv = new ModelAndView("signup");
         mv.addObject("error", error);
         return mv;
      }
      mv.addObject("signupInfo", signUpForm);
      mv.addObject("cloudName", cloudName);
      return mv;
   }

}
