package net.respectnetwork.csp.application.controller;

import com.google.common.base.Splitter;
import net.respectnetwork.csp.application.dao.DAOException;
import net.respectnetwork.csp.application.dao.DAOFactory;
import net.respectnetwork.csp.application.dao.SignupInfoDAO;
import net.respectnetwork.csp.application.exception.CSPException;
import net.respectnetwork.csp.application.exception.CSPProValidationException;
import net.respectnetwork.csp.application.exception.UserRegistrationException;
import net.respectnetwork.csp.application.form.DependentForm;
import net.respectnetwork.csp.application.form.InviteForm;
import net.respectnetwork.csp.application.form.LoginForm;
import net.respectnetwork.csp.application.form.PaymentForm;
import net.respectnetwork.csp.application.form.SignUpForm;
import net.respectnetwork.csp.application.invite.InvitationManager;
import net.respectnetwork.csp.application.manager.BrainTreePaymentProcessor;
import net.respectnetwork.csp.application.manager.PersonalCloudManager;
import net.respectnetwork.csp.application.manager.PinNetAuPaymentProcessor;
import net.respectnetwork.csp.application.manager.RegistrationManager;
import net.respectnetwork.csp.application.manager.SagePayPaymentProcessor;
import net.respectnetwork.csp.application.manager.StripePaymentProcessor;
import net.respectnetwork.csp.application.model.CSPModel;
import net.respectnetwork.csp.application.model.GiftCodeModel;
import net.respectnetwork.csp.application.model.GiftCodeRedemptionModel;
import net.respectnetwork.csp.application.model.InviteModel;
import net.respectnetwork.csp.application.model.PaymentModel;
import net.respectnetwork.csp.application.model.PromoCloudModel;
import net.respectnetwork.csp.application.model.PromoCodeModel;
import net.respectnetwork.csp.application.model.SignupInfoModel;
import net.respectnetwork.csp.application.session.RegistrationSession;
import net.respectnetwork.csp.application.types.PaymentType;
import net.respectnetwork.csp.application.util.ResponseBuilder;
import net.respectnetwork.sdk.csp.CSP;
import net.respectnetwork.sdk.csp.exception.CSPRegistrationException;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.core.xri3.CloudName;
import xdi2.core.xri3.CloudNumber;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.toArray;
import static net.respectnetwork.csp.application.constants.PaymentGateway.*;
import static net.respectnetwork.csp.application.controller.RegistrationController.formatCurrencyAmount;

@Controller
public class PersonalCloudController
{

   /** CLass Logger */
   private static final Logger  logger = LoggerFactory
                                             .getLogger(PersonalCloudController.class);

   /**
    * Invitation Service : to create invites and gift codes
    */
   private InvitationManager    invitationManager;

   /**
    * Registration Service : to register dependent clouds
    */
   private RegistrationManager  registrationManager;

   /**
    * Personal Cloud Service : to authenticate to personal cloud and get/set
    * information from/to the personal cloud
    */
   private PersonalCloudManager personalCloudManager;

   /** Registration Session */
   private RegistrationSession  regSession;

   /**
    * CSP Cloud Name
    */
   private String               cspCloudName;

   @Autowired
   private MessageSource messageSource;

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
    * @return the invitationManager
    */
   public InvitationManager getInvitationManager()
   {
      return invitationManager;
   }

   /**
    * @param invitationManager
    *           the invitationManager to set
    */
   @Autowired
   @Required
   public void setInvitationManager(InvitationManager invitationManager)
   {
      this.invitationManager = invitationManager;
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

   @RequestMapping(value = "/login", method = RequestMethod.GET)
   public ModelAndView showLoginForm(HttpServletRequest request, Model model)
   {
      logger.info("showing login form");

      ModelAndView mv = null;
      if (regSession != null)
      {
         regSession.setCloudName(null);
         regSession.setPassword(null);
         regSession.setVerifiedEmail(null);
         regSession.setDependentForm(null);
         regSession.setGiftCode(null);
         regSession.setInviteCode(null);
         regSession.setInviteForm(null);
         regSession.setSessionId(null);
         regSession.setVerifiedMobilePhone(null);
      }

      String cspHomeURL = request.getContextPath();
      String formPostURL = cspHomeURL + "/cloudPage";

      mv = new ModelAndView("login");
      mv.addObject("postURL", formPostURL);
      return mv;
   }

    @RequestMapping(value = "/cloudPage", method = { RequestMethod.POST, RequestMethod.GET })
    public ModelAndView showCloudPage(@ModelAttribute("loginInfo") LoginForm loginForm,
                                      HttpServletRequest request) {
        logger.info("showing cloudPage form");

        ResponseBuilder resp = new ResponseBuilder(messageSource, request)
                .setView("login")
                .addObject("loginInfo", new LoginForm(loginForm));

        // Require cloud name
        String cloudNameVal = loginForm.getCloudName();
        if (isNullOrEmpty(cloudNameVal)) {
            cloudNameVal = (regSession != null) ? regSession.getCloudName() : null;
        }
        if (isNullOrEmpty(cloudNameVal)) {
            resp.addError("cloudName", "login.msg.cloudName.required");
        }

        // Require password
        String passwordVal = null;
        if (regSession != null) {
            passwordVal = regSession.getPassword();
        }
        if (isNullOrEmpty(passwordVal)) {
            passwordVal =  loginForm.getPassword();
        }
        if (isNullOrEmpty(passwordVal)) {
            resp.addError("password", "login.msg.password.required");
        }

        // Show required errors
        if (resp.hasErrors()) {
            processLogout(request);
            return resp.build();
        }

        // Validate cloud name
        //noinspection ConstantConditions
        if (!cloudNameVal.startsWith("=")) {
            cloudNameVal = "=" + cloudNameVal;
        }
        if (regSession.getCloudName() != null && !cloudNameVal.equalsIgnoreCase(regSession.getCloudName())) {
            resp.addError("cloudName", "login.msg.cloudName.required");
            resp.addObject("loginInfo", new LoginForm(loginForm.getRedirect())); // remove non-matching cloud name
        } else  if(!RegistrationManager.validateCloudName(cloudNameVal) ) {
            resp.addError("cloudName", "signUp.msg.invalid");
        }
        if (resp.hasErrors()) {
            processLogout(request);
            return resp.build();
        }

        // Get CSP registrar
        CSP cspRegistrar = registrationManager.getCspRegistrar();
        if (cspRegistrar == null) { // can this actually happen??
            logger.error("cspRegistrar is null!");
            return resp.addGeneralError("error.unknown").build();
        }

        // Process login
        CloudName cloudName = CloudName.create(cloudNameVal);
        logger.debug("Logging in for cloudName={}, cspRegistrar={}", cloudName, cspRegistrar);

        CloudNumber cloudNumber = null;
        try {
            cloudNumber = cspRegistrar.checkCloudNameInRN(cloudName);
            if (cloudNumber != null) {
                cspRegistrar.authenticateInCloud(cloudNumber, passwordVal);

                if (regSession != null && isNullOrEmpty(regSession.getCloudName())) {

                    if (isNullOrEmpty(regSession.getSessionId())) {
                        String sessionId = UUID.randomUUID().toString();
                        logger.debug("Creating a new regSession :: sessionId={}", sessionId);
                        regSession.setSessionId(sessionId);
                    }

                    logger.debug("Updating regSession :: cloudName={}", cloudNameVal);
                    regSession.setCloudName(cloudNameVal);
                    regSession.setPassword(passwordVal);

                    // Retrieve verified phone and email
                    SignupInfoDAO signupInfoDAO = DAOFactory.getInstance().getSignupInfoDAO();
                    try {
                        SignupInfoModel signupInfo = signupInfoDAO.get(regSession.getCloudName());
                        if (signupInfo != null) {
                            regSession.setVerifiedEmail(signupInfo.getEmail());
                            regSession.setVerifiedMobilePhone(signupInfo.getPhone());
                        }
                    } catch (DAOException e) {
                        // todo should this fail the login process??
                        logger.error("Error getting signupInfo", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to process login :: cloudNumber={}, loginForm={}", cloudNumber, loginForm, e);
            cloudNumber = null;
        }

        // Response
        if (cloudNumber == null) {
            logger.debug("Authenticating to personal cloud failed :: loginForm={}", loginForm);
            return resp.addGeneralError("login.msg.invalid").build();
        } else {
            logger.info("Successfully authenticated to the personal cloud :: loginForm={}", loginForm);
            if (loginForm.hasRedirect()) {
                return new ModelAndView("redirect:" + loginForm.getRedirect());
            } else {
                return getCloudPage(request, regSession.getCloudName());
            }
        }
   }

   public RegistrationManager getRegistrationManager()
   {
      return registrationManager;
   }

   @Autowired
   public void setRegistrationManager(RegistrationManager registrationManager)
   {
      this.registrationManager = registrationManager;
   }

   public PersonalCloudManager getPersonalCloudManager()
   {
      return personalCloudManager;
   }

   @Autowired
   public void setPersonalCloudManager(PersonalCloudManager personalCloudManager)
   {
      this.personalCloudManager = personalCloudManager;
   }

   @RequestMapping(value = "/logout", method =
   { RequestMethod.GET, RequestMethod.POST })
   public ModelAndView processLogout(HttpServletRequest request)
   {
      logger.info("processing logout");

      // nullify password from the session object
      if (regSession != null)
      {
         regSession.setCloudName(null);
         regSession.setPassword(null);
         regSession.setVerifiedEmail(null);
         regSession.setDependentForm(null);
         regSession.setGiftCode(null);
         regSession.setInviteCode(null);
         regSession.setInviteForm(null);
         regSession.setSessionId(null);
         regSession.setVerifiedMobilePhone(null);
      }
      ModelAndView mv = null;

      String cspHomeURL = request.getContextPath();
      String formPostURL = cspHomeURL + "/cloudPage";

      mv = new ModelAndView("login");
      mv.addObject("postURL", formPostURL);
      return mv;
   }

   @RequestMapping(value = "/ccpayment", method =
   { RequestMethod.POST, RequestMethod.GET })
   public ModelAndView processCCPayment(
         @Valid @ModelAttribute("paymentInfo") PaymentForm paymentForm,
         HttpServletRequest request, HttpServletResponse response, Model model,
         BindingResult result)
   {
      logger.info("processing CC payment");

      boolean errors = false;
      String errorText = "";

      String cloudName = regSession.getCloudName();

      ModelAndView mv = null;

      String sessionIdentifier = regSession.getSessionId();
      String email = regSession.getVerifiedEmail();
      String phone = regSession.getVerifiedMobilePhone();
      String password = regSession.getPassword();
      logger.debug(sessionIdentifier + "--" + cloudName + "--" + email + "--"
            + phone + "--" + password);

      String txnType = paymentForm.getTxnType();
      if (txnType == null || txnType.isEmpty())
      {
         txnType = regSession.getTransactionType();
      }

      logger.debug("Transaction type = " + txnType);

      // Check Session
      if (sessionIdentifier == null || cloudName == null || password == null)
      {
         errors = true;
         errorText = "Invalid Session";

         logger.debug("Invalid Session ...");
      }
      CSPModel cspModel = null;
      DAOFactory dao = DAOFactory.getInstance();
      try
      {
         cspModel = dao.getCSPDAO().get(this.getCspCloudName());
      } catch (DAOException e1)
      {
         // TODO Auto-generated catch block
         e1.printStackTrace();
         errors = true;
         errorText = "Cannot connect to DB to lookup information";

         logger.debug("Cannot connect to DB to lookup info...");
      }

      String forwardingPage = request.getContextPath();
      String method = "post";
      String queryStr = "";
      String statusText = "";

      if (!errors)
      {

         String currency = regSession.getCurrency();
         BigDecimal amount = null;
         if (cspModel.getPaymentGatewayName().equals("STRIPE")
               || cspModel.getPaymentGatewayName().equals("BRAINTREE")
               || cspModel.getPaymentGatewayName().equals(
                     PinNetAuPaymentProcessor.DB_PAYMENT_GATEWAY_NAME))
         {
            // TODO - check numberofClouds > 0
            amount = regSession.getCostPerCloudName().multiply(
                  new BigDecimal(paymentForm.getNumberOfClouds()));
            logger.debug("Charging CC for " + amount.toPlainString());
            logger.debug("Number of clouds being purchased "
                  + paymentForm.getNumberOfClouds());
         }

         PaymentModel payment = null;
         if (cspModel.getPaymentGatewayName().equals("STRIPE"))
         {
            String desc = "";
            if (txnType.equals(PaymentForm.TXN_TYPE_SIGNUP))
            {
               desc = "Personal cloud for " + cloudName;
            } else if (txnType.equals(PaymentForm.TXN_TYPE_BUY_GC))
            {
               desc = paymentForm.getNumberOfClouds() + " giftcodes for "
                     + cloudName;
            }
            String token = StripePaymentProcessor.getToken(request);
            payment = StripePaymentProcessor.makePayment(cspModel, amount,
                    currency, desc, token);
         } else if (cspModel.getPaymentGatewayName().equals("BRAINTREE"))
         {
            payment = BrainTreePaymentProcessor.makePayment(cspModel, amount,
                  currency, regSession.getMerchantAccountId(), request);
         } else if (cspModel.getPaymentGatewayName().equals(
               PinNetAuPaymentProcessor.DB_PAYMENT_GATEWAY_NAME))
         {
            String cardToken = request.getParameter("card_token");
            payment = PinNetAuPaymentProcessor.makePayment(cspModel, amount,
                  currency, null, email, request.getRemoteAddr(), cardToken);
         } else if (cspModel.getPaymentGatewayName().equals("SAGEPAY"))
         {
            payment = SagePayPaymentProcessor.processSagePayCallback(request,
                  response, cspModel, currency);
         }

         if (payment != null)
         {

            try
            {
               dao.getPaymentDAO().insert(payment);
            } catch (DAOException e1)
            {
               logger.error("Could not insert payment record in the DB "
                     + e1.getMessage());
               logger.info("Payment record info \n" + payment.toString());
            }

            if (txnType.equals(PaymentForm.TXN_TYPE_SIGNUP))
            {
               if (this.registerCloudName(cloudName, phone, email, password, PaymentType.CreditCard.toString(), payment.getPaymentId(), request.getLocale()))
               {
                  forwardingPage = getRNpostRegistrationLandingPage() ; //RegistrationManager.getCspInviteURL();
                  queryStr = this.formatQueryStr(cloudName, regSession.getRnQueryString(), request);
                  statusText = "Thank you " + cloudName + " for your personal cloud order."
                          + "We will shortly notify once we complete registering your cloud with the network.";
               } else
               {
                  forwardingPage += "/signup";
                  statusText = "Sorry! The system encountered an error while registering your cloud name.\n"
                        + registrationManager.getCSPContactInfo();

               }
            } else if (txnType.equals(PaymentForm.TXN_TYPE_DEP)) 
            {
               if ((mv = createDependentClouds(cloudName, payment, null,
                     "",request)) != null)
               {
                  // forwardingPage += "/cloudPage";
                  forwardingPage = getRNpostRegistrationLandingPage() ; //RegistrationManager.getCspInviteURL();
                  queryStr = this.formatQueryStr(cloudName, regSession.getRnQueryString(), request);
                  statusText = "Thank you " + cloudName + " for your dependent cloud order."
                          + "We will shortly notify once we complete registering your cloud with the network.";
               } else
               {
                  forwardingPage += "/cloudPage";
                  statusText = "Sorry! The system encountered an error while registering dependent clouds.\n"
                        + registrationManager.getCSPContactInfo();

               }

            } else if (txnType.equals(PaymentForm.TXN_TYPE_BUY_GC))
            {
               logger.debug("Going to create gift cards now for " + cloudName);
               if ((mv = this.createGiftCards(request, cloudName, payment,
                     cspModel)) != null)
               {
                  // forwardingPage += "/cloudPage";
                  forwardingPage = getRNpostRegistrationLandingPage() ; //RegistrationManager.getCspInviteURL();
                  queryStr = this.formatQueryStr(cloudName, regSession.getRnQueryString(), request);
                  statusText = "Congratulations " + cloudName
                        + "! You have successfully purchased giftcodes.";
               } else
               {
                  forwardingPage += "/cloudPage";
                  statusText = "Sorry! The system encountered an error while purchasing giftcodes.\n"
                        + registrationManager.getCSPContactInfo();
               }
            } else
            {
               forwardingPage += "/login";
               statusText = "Sorry! Something bad happened while processing your request. Returning you to login page. Please try again.\n"
                     + registrationManager.getCSPContactInfo();
            }

         } else
         {
            forwardingPage += "/signup";
            statusText = "Sorry ! Payment Processing Error";
         }
      }

      mv = new ModelAndView("AutoSubmitForm"); // DO NOT CHANGE THE REASSIGNMENT
                                               // OF THE VIEW HERE
      mv.addObject("URL", request.getContextPath()
            + "/transactionSuccessFailure");
      mv.addObject("cloudName", cloudName);

      mv.addObject("statusText", statusText);
      mv.addObject("nextHop", forwardingPage);
      mv.addObject("submitMethod", method);
      mv.addObject("queryStr", queryStr);

      return mv;
   }

   public static ModelAndView getCloudPage(HttpServletRequest request,
         String cloudName)
   {

      // logger.debug("Request servlet path " + request.getServletPath());
      // logger.debug("Paths " + request.getPathInfo() + "-" +
      // request.getRequestURI() + "-" + request.getPathTranslated() );
      ModelAndView mv = new ModelAndView("cloudPage");

      String cspHomeURL = request.getContextPath();
      // logger.debug("getCloudPage :: cspHomeURL " + cspHomeURL);
      mv.addObject("logoutURL", cspHomeURL + "/logout");
      mv.addObject("cloudName", cloudName);
      String queryStr = "";
      try
      {
         queryStr = "name="
               + URLEncoder.encode(cloudName, "UTF-8");
         
          queryStr += "&csp="
               + URLEncoder.encode(request.getContextPath().replace("/", "+"),
                     "UTF-8");
         
         queryStr += "&inviter=" + URLEncoder.encode(cloudName, "UTF-8");
      } catch (UnsupportedEncodingException e1)
      {
         // TODO Auto-generated catch block
         e1.printStackTrace();
      }
      mv.addObject("queryStr", queryStr);
      mv.addObject("postURL", RegistrationManager.getPostRegistrationURL());      
      return mv;
   }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(PaymentType.class, new PaymentType.Converter());
    }

    @RequestMapping(value = "/makePayment", method = RequestMethod.POST)
    public ModelAndView makePayment(
            @Valid @ModelAttribute("paymentInfo") PaymentForm paymentFormIn,
            HttpServletRequest request,
            HttpServletResponse response,
            BindingResult result) {
        logger.info("processing makePayment :: bindingResult={}", result);

        PaymentForm paymentForm = new PaymentForm(paymentFormIn);

        ResponseBuilder form = new ResponseBuilder(messageSource, request)
                .setView("payment")
                .setCloudName(regSession.getCloudName())
                .addObject("paymentInfo", paymentForm)
                .addObject("totalAmountText", formatCurrencyAmount(regSession.getCurrency(), regSession.getCostPerCloudName()));

        // Validate session
        String sessionId = regSession.getSessionId();
        String cloudName = regSession.getCloudName();
        if (isNullOrEmpty(sessionId) || isNullOrEmpty(cloudName)) {
            logger.debug("Invalid sessionId or cloudName :: sessionId={}, cloudName={}", sessionId, cloudName);
            return form.addGeneralError("form.invalidSession").build();
        }

        // Validate transaction type
        String txnType = paymentForm.getTxnType();
        if (isNullOrEmpty(txnType) || (!PaymentForm.TXN_TYPE_SIGNUP.equals(txnType)
                && !PaymentForm.TXN_TYPE_BUY_GC.equals(txnType)
                && !PaymentForm.TXN_TYPE_DEP.equals(txnType))) {
            return form.addGeneralError("payments.msg.invalidTransaction").build();
        }

        // Validate number of clouds
        int cloudCount = paymentForm.getNumberOfClouds();
        if (cloudCount <= 0) {
            return form.addGeneralError("payments.msg.cloudCount.invalid").build();
        }

        // Check availability of cloud name before re-directing user to payment page (PCLOD-181)
        try {
            validateCloudNameAvailability(cloudName, txnType);
        } catch (CSPProValidationException ex) {
            logger.error("Sorry! The dependent cloud name is not available.", ex);
            ModelAndView mv = null;
            if (PaymentForm.TXN_TYPE_DEP.equals(txnType)) {
                mv = new ModelAndView("dependent");
                mv.addObject("error", ex.getMessage());
                mv.addObject("cloudName", cloudName);
                mv.addObject("dependentForm", regSession.getDependentForm());
            } else if (PaymentForm.TXN_TYPE_SIGNUP.equals(txnType)) {
                return new ResponseBuilder(messageSource, request)
                        .setView("signup")
                        .addObject("signUpInfo", new SignUpForm(regSession.getCloudName()))
                        .addGeneralError("signUp.msg.unavailable")
                        .build();
            }
            return mv;

        } catch (Exception e) {
            ModelAndView mv = null;
            String errorText;
            if (PaymentForm.TXN_TYPE_DEP.equals(txnType)) {
                errorText = "Sorry! The system encountered an error while registering dependent clouds.\n"
                        + registrationManager.getCSPContactInfo();
                mv = new ModelAndView("dependent");
                mv.addObject("error", errorText);
                mv.addObject("cloudName", cloudName);
                mv.addObject("dependentForm", regSession.getDependentForm());
            } else if (PaymentForm.TXN_TYPE_SIGNUP.equals(txnType)) {
                return new ResponseBuilder(messageSource, request)
                        .setView("signup")
                        .addObject("signUpInfo", new SignUpForm(regSession.getCloudName()))
                        .addGeneralError("signUp.msg.nameCheckError")
                        .build();
            }
            return mv;
        }

        // Load CSP Model
        CSPModel cspModel = null;
        try {
            cspModel = DAOFactory.getInstance().getCSPDAO().get(cspCloudName);
            if (cspModel == null) {
                form.addGeneralError("form.databaseError");
            }
        } catch (Exception e) {
            logger.error("Failed to get cspModel :: cspCloudName={}", cspCloudName);
            form.addGeneralError("form.databaseError");
        }
        if (form.hasErrors()) {
            return form.build();
        }

        // Validate Payment Type
        String giftCodesVal = paymentForm.getGiftCodes();
        PaymentType paymentType = paymentForm.getPaymentType();
        if (GIFT_CODE_ONLY.is(cspModel)) {
            paymentForm.setGiftCodesOnly(true);
        }
        if (paymentType == null) {
            logger.debug("No payment type selected :: paymentForm={}", paymentForm);
            form.addError("paymentType", "selectPayment.msg.type.required");
        } else if (paymentForm.isGiftCodesOnly() &&
                (paymentType != PaymentType.GiftCode || isNullOrEmpty(giftCodesVal))) {
            logger.debug("Gift codes required but missing :: paymentForm={}", paymentForm);
            form.addError("giftCodes", "selectPayment.msg.giftCodes.required");
        } else if (paymentType == PaymentType.GiftCode && isNullOrEmpty(giftCodesVal)) {
            logger.debug("Gift codes selected but empty :: paymentForm={}", paymentForm);
            form.addError("giftCodes", "selectPayment.msg.giftCodes.required");
        } else if (paymentType != PaymentType.GiftCode && !isNullOrEmpty(giftCodesVal)) {
            logger.debug("Gift codes submitted but option not selected :: paymentForm={}", paymentForm);
            form.addError("giftCodes", "selectPayment.msg.giftCodes.accident");
        }

        // Validate Terms and Conditions
        if(!paymentForm.isTermsChecked()) {
            logger.debug("Respect Trust Framework not checked");
            form.addError("terms", "form.msg.terms.required");
        }

        // Show any errors
        if (form.hasErrors()) {
            return form.build();
        }

        // Process payment types
        if (paymentType == PaymentType.GiftCode && !isNullOrEmpty(giftCodesVal)) {
            if (giftCodesVal.toLowerCase().startsWith("promo")) { // Promo gift code
                return processPromoGiftCode(form, txnType, giftCodesVal, request);
            } else { // Normal gift codes
                return processGiftCodes(form, paymentForm, request, cspModel);
            }
        } else { // Credit Card Payment
            return processCreditType(form, paymentForm, request, cspModel);
        }
   }

    private ModelAndView processCreditType(ResponseBuilder form, PaymentForm paymentForm, HttpServletRequest req, CSPModel cspModel) {
        logger.debug("Going to show the CC payment screen now.");

        ModelAndView mv = new ModelAndView("creditCardPayment");
        mv.addObject("cspModel", cspModel);
        mv.addObject("paymentInfo", paymentForm);

        BigDecimal amount = regSession.getCostPerCloudName().multiply(new BigDecimal(paymentForm.getNumberOfClouds()));
        mv.addObject("amount", amount.toPlainString());
        mv.addObject("totalAmountText", formatCurrencyAmount(regSession.getCurrency(), amount));

        String cspHomeURL = req.getContextPath();
        if (STRIPE.is(cspModel)) {
            logger.debug("Payment gateway is STRIPE");
            String desc = "Personal cloud  " + regSession.getCloudName();
            mv.addObject("StripeJavaScript", StripePaymentProcessor.getJavaScript(cspModel, amount, desc));
            mv.addObject("postURL", cspHomeURL + "/ccpayment");
        } else if (SAGE_PAY.is(cspModel)) {
            logger.debug("Payment gateway is SAGEPAY");
            mv.addObject("postURL", cspHomeURL + "/submitCustomerDetail");
            mv.addObject("SagePay", "SAGEPAY");
        } else if (BRAIN_TREE.is(cspModel)) {
            logger.debug("Payment gateway is BRAINTREE");
            mv.addObject("BrainTree", BrainTreePaymentProcessor.getJavaScript(cspModel));
            mv.addObject("postURL", cspHomeURL + "/ccpayment");
        } else if (PIN_NET_AU.is(cspModel)) {
            logger.debug("Payment gateway is PIN");
            mv.addObject("PinNetAu", PinNetAuPaymentProcessor.DB_PAYMENT_GATEWAY_NAME);
            mv.addObject("publishableKey", PinNetAuPaymentProcessor.getPublishableApiKey(cspModel));
            mv.addObject("environment", PinNetAuPaymentProcessor.getEnvironment(cspModel));
            mv.addObject("postURL", cspHomeURL + "/ccpayment");
        } else {
            return form.addGeneralError("Unsupported payment processor").build();
        }

        return mv;
    }

    private ModelAndView processGiftCodes(ResponseBuilder form, PaymentForm paymentForm, HttpServletRequest req, CSPModel cspModel) {

        String giftCodesVal = paymentForm.getGiftCodes();
        List<String> giftCodes = Splitter.on(' ')
                .trimResults()
                .omitEmptyStrings()
                .splitToList(giftCodesVal);

        if (giftCodes.isEmpty()) {
            return form.addError("giftCodes", "selectPayment.msg.giftCodes.required").build();
        }
        if (paymentForm.getNumberOfClouds() < giftCodes.size()) {
            return form.addError("giftCodes", "selectPayment.msg.giftCodes.tooMany", paymentForm.getNumberOfClouds()).build();
        }

        DAOFactory dao = DAOFactory.getInstance();
        for (String giftCode : giftCodes) {
            logger.debug("Processing gift code {}", giftCode);

            try {
                GiftCodeModel giftCodeObj = dao.getGiftCodeDAO().get(giftCode);
                if (giftCodeObj != null) {
                    GiftCodeRedemptionModel gcrObj = dao.getGiftCodeRedemptionDAO().get(giftCode);
                    if (gcrObj != null) {
                        form.addError("giftCodes", "selectPayment.msg.giftCodes.used", giftCode);
                    }
                } else {
                    form.addError("giftCodes", "selectPayment.msg.giftCodes.invalid", giftCode);
                }
            } catch (Exception e) {
                form.addError("giftCodes", "selectPayment.msg.giftCodes.invalid", giftCode);
            }
        }

        if (form.hasErrors()) {
            return form.build();
        }

        // update registration info
        regSession.setGiftCode(giftCodesVal);

        ModelAndView mv;
        String method = "post";
        String forwardingPage = "";
        String queryStr = "";
        String statusText = "";

        String cloudName = regSession.getCloudName();
        String email = regSession.getVerifiedEmail();
        String phone = regSession.getVerifiedMobilePhone();
        String password = regSession.getPassword();

        // need a new unique response id
        String responseId = UUID.randomUUID().toString();
        String txnType = paymentForm.getTxnType();

        // make entries in the gift code_redemption table that a new cloud has been registered against a gift code
        if (PaymentForm.TXN_TYPE_SIGNUP.equals(txnType)) {
            String giftCode = giftCodes.get(0);
            if (registerCloudName(cloudName, phone, email, password, PaymentType.GiftCode.toString(), giftCode, req.getLocale())) {
                logger.debug("Going to create the personal cloud now for gift code path...");

                forwardingPage = getRNpostRegistrationLandingPage();
                queryStr = formatQueryStr(cloudName, regSession.getRnQueryString(), req);
                statusText = "Thank you " + cloudName + " for your personal cloud order."
                        + "We will shortly notify once we complete registering your cloud with the network.";

                // make a new record in the giftcode_redemption table
                GiftCodeRedemptionModel giftCodeRedemption = new GiftCodeRedemptionModel();
                giftCodeRedemption.setCloudNameCreated(cloudName);
                giftCodeRedemption.setGiftCodeId(regSession.getGiftCode());
                giftCodeRedemption.setRedemptionId(responseId);
                giftCodeRedemption.setTimeCreated(new Date());

                try {
                    dao.getGiftCodeRedemptionDAO().insert(giftCodeRedemption);
                } catch (DAOException e) {
                    logger.error("Gift code redemption entry failed :: cloudName={}, giftCode={}", cloudName, regSession
                            .getGiftCode(), e);
                }
            } else {
                return form.addGeneralError("error.register.cloudName", registrationManager.getCSPContactInfo()).build();
            }
        } else if (PaymentForm.TXN_TYPE_DEP.equals(txnType)) {
            ModelAndView mvResult = createDependentClouds(cloudName, null, toArray(giftCodes, String.class), "", req);
            if (mvResult == null) {
                return form.addGeneralError("error.register.cloudName.dependent", registrationManager.getCSPContactInfo()).build();
            }

            if ("dependentDone".equals(mvResult.getViewName())) { // all dependents have been paid for
                forwardingPage = getRNpostRegistrationLandingPage();
                queryStr = formatQueryStr(cloudName, regSession.getRnQueryString(), req);
                statusText = "Thank you " + cloudName + " for your dependent cloud order."
                        + "We will shortly notify once we complete registering your cloud with the network.";
            } else {
                paymentForm.setNumberOfClouds(paymentForm.getNumberOfClouds() - giftCodes.size());
                return processCreditType(form, paymentForm, req, cspModel); // additional credit needed
            }
        }

        mv = new ModelAndView("AutoSubmitForm");
        mv.addObject("URL", req.getContextPath() + "/transactionSuccessFailure");
        mv.addObject("cloudName", cloudName);
        mv.addObject("submitMethod", method);
        mv.addObject("statusText", statusText);
        mv.addObject("nextHop", forwardingPage);
        mv.addObject("queryStr", queryStr);

        return mv;
    }

    private ModelAndView processPromoGiftCode(ResponseBuilder form, String txnType, String giftCodesVal, HttpServletRequest req) {
        DAOFactory dao = DAOFactory.getInstance();

        PromoCodeModel promo = null;
        try {
            promo = dao.getPromoCodeDAO().get(giftCodesVal.toUpperCase());
        } catch (Exception e) {
            logger.error("Failed to get promo :: giftCodeVal={}", giftCodesVal, e);
        }
        if (promo == null) {
            return form.addError("giftCodes", "selectPayment.msg.giftCodes.promo.invalid").build();
        }

        ModelAndView mv;
        String method = "post";
        String forwardingPage = "";
        String queryStr = "";
        String statusText = "";

        String cloudName = regSession.getCloudName();
        String email = regSession.getVerifiedEmail();
        String phone = regSession.getVerifiedMobilePhone();
        String password = regSession.getPassword();

        if (PaymentForm.TXN_TYPE_SIGNUP.equals(txnType)) {
            if (registerCloudName(cloudName, phone, email, password, PaymentType.PromoCode.toString(), giftCodesVal, req
                    .getLocale())) {
                forwardingPage = getRNpostRegistrationLandingPage();
                queryStr = formatQueryStr(cloudName, regSession.getRnQueryString(), req);
                statusText = "Thank you " + cloudName + " for your personal cloud order."
                        + "We will shortly notify once we complete registering your cloud with the network.";

                // make an entry in promo_cloud table
                PromoCloudModel promoCloud = new PromoCloudModel();
                promoCloud.setCloudname(cloudName);
                promoCloud.setPromo_id(giftCodesVal.toUpperCase());
                promoCloud.setCsp_cloudname(this.getCspCloudName());
                try {
                    dao.getPromoCloudDAO().insert(promoCloud);
                } catch (DAOException e) {
                    logger.error("Promo code redemption entry failed :: cloudName={}, promoCode={}", cloudName, giftCodesVal, e);
                }
            } else {
                return form.addGeneralError("error.register.cloudName", registrationManager.getCSPContactInfo()).build();
            }
        } else if (PaymentForm.TXN_TYPE_DEP.equals(txnType)) {
            ModelAndView mvResult = createDependentClouds(cloudName, null, null, giftCodesVal, req);
            if (mvResult == null) {
                return form.addGeneralError("error.register.cloudName.dependent", registrationManager.getCSPContactInfo()).build();
            }

            if ("dependentDone".equals(mvResult.getViewName())) { // all dependents have been paid for
                forwardingPage = getRNpostRegistrationLandingPage();
                queryStr = formatQueryStr(cloudName, regSession.getRnQueryString(), req);
                statusText = "Thank you " + cloudName + " for your dependent cloud order."
                        + "We will shortly notify once we complete registering your cloud with the network.";
            } else {
                return form.addGeneralError("error.register.cloudName.dependent", registrationManager.getCSPContactInfo()).build();
            }
        }

        mv = new ModelAndView("AutoSubmitForm");
        mv.addObject("URL", req.getContextPath() + "/transactionSuccessFailure");
        mv.addObject("cloudName", cloudName);
        mv.addObject("submitMethod", method);
        mv.addObject("statusText", statusText);
        mv.addObject("nextHop", forwardingPage);
        mv.addObject("queryStr", queryStr);

        return mv;
    }

     /**
     * 
     * @param cloudName
     * @param txnType
     * @throws UserRegistrationException
     * @throws CSPException
     */
    private void validateCloudNameAvailability(String cloudName, String txnType)
            throws UserRegistrationException, CSPException {
        if (PaymentForm.TXN_TYPE_SIGNUP.equals(txnType)) {
            // check availability of cloud name before billing once again
            if (!registrationManager.isCloudNameAvailableInRegistry(cloudName)
                    && !registrationManager.isCloudNameAvailable(cloudName)) {
                throw new CSPProValidationException(
                        "Sorry! The cloud name is not available. Please try with some different cloud name.");
            }
        }
        if (PaymentForm.TXN_TYPE_DEP.equals(txnType)) {
            DependentForm dependentForm = regSession.getDependentForm();
            if (dependentForm != null
                    && dependentForm.getDependentCloudName().size() > 0) {
                ArrayList<String> dependentCloudNameList = dependentForm
                        .getDependentCloudName();
                for (String dependentCloudName : dependentCloudNameList) {
                    // check availability of cloud name before billing once
                    // again
                    if (!registrationManager
                            .isCloudNameAvailableInRegistry(dependentCloudName)
                            && !registrationManager
                                    .isCloudNameAvailable(dependentCloudName)) {
                        throw new CSPProValidationException(
                                "Sorry! The dependent cloud name is not available. Please try with some different cloud name.");
                    }
                }
            }
        }
    }

private ModelAndView createDependentClouds(String cloudName,
         PaymentModel payment, String[] giftCodes, String promoCode , HttpServletRequest request)
   {
      ModelAndView mv = null;
      boolean errors = false;
      DAOFactory dao = DAOFactory.getInstance();

      DependentForm dependentForm = regSession.getDependentForm();
      if(dependentForm == null)
      {
         return null;
      }
      ArrayList<String> arrDependentCloudName = dependentForm.getDependentCloudName();
      ArrayList<String> arrDependentCloudPasswords = dependentForm
            .getDependentCloudPassword();
      ArrayList<String> arrDependentCloudBirthDates = dependentForm
            .getDependentBirthDate();

      int cloudsPurchasedWithGiftCodes = 0;
      if (payment != null) // payment via CC
      {
         String giftCodeStr = regSession.getGiftCode();
         if (giftCodeStr != null && !giftCodeStr.isEmpty())
         {
            cloudsPurchasedWithGiftCodes = giftCodeStr.split(" ").length;
         }
      }
      // register the dependent cloudnames

      String guardianEmailAddress = getRegisteredEmailAddress();
      String guardianPhoneNumber = getRegisteredPhoneNumber();
      int i = 0;
      for (String dependentCloudName : arrDependentCloudName)
      {
         if(promoCode == null || promoCode.isEmpty())
         {
            if (giftCodes != null && i == giftCodes.length)
            {
               break;
            }
            if (i < cloudsPurchasedWithGiftCodes)
            {
               i++;
               continue;
            }
         }
         if (i >= arrDependentCloudName.size())
         {
            break;
         }
         String paymentType = "";
         String paymentRefId = "";
         if (payment != null) {
             paymentType = PaymentType.CreditCard.toString();
             paymentRefId = payment.getPaymentId();
         } else if (giftCodes != null && giftCodes.length > 0) {
             paymentType = PaymentType.GiftCode.toString();
             paymentRefId = giftCodes[i];
         } else if(promoCode != null && !promoCode.isEmpty()) {
             paymentType = PaymentType.PromoCode.toString();
             paymentRefId = promoCode;
         }
         logger.debug("Creating dependent cloud for " + dependentCloudName);
         CloudNumber dependentCloudNumber = registrationManager
               .registerDependent(CloudName.create(cloudName),
                     regSession.getPassword(),
                     CloudName.create(dependentCloudName),
                     arrDependentCloudPasswords.get(i),
                     arrDependentCloudBirthDates.get(i),
                     paymentType,
                     paymentRefId,
                     guardianEmailAddress,
                     guardianPhoneNumber,
                     request.getLocale());
         if (dependentCloudNumber != null)
         {
            logger.info("Dependent Cloud Number "
                  + dependentCloudNumber.toString());
            if (payment != null)
            {
               PersonalCloudDependentController.saveDependent(
                     dependentCloudName, payment, cloudName, null);
            } else if (giftCodes != null && giftCodes.length > 0)
            {
               PersonalCloudDependentController.saveDependent(
                     dependentCloudName, null, cloudName, giftCodes[i]);
            } else if(promoCode != null && !promoCode.isEmpty())
            {
               PersonalCloudDependentController.saveDependent(
                     dependentCloudName, null, cloudName, promoCode);
               // make an entry in promo_cloud table
               PromoCloudModel promoCloud = new PromoCloudModel();
               promoCloud.setCloudname(dependentCloudName);
               promoCloud.setPromo_id(promoCode);
               promoCloud.setCsp_cloudname(this.getCspCloudName());
               try
               {
                  dao.getPromoCloudDAO().insert(promoCloud);
               } catch (DAOException e)
               {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }     
            }
         } else
         {
            logger.error("Dependent Cloud Could not be registered");
            errors = true;
         }
         if (payment == null && giftCodes != null && giftCodes.length > 0)
         {
            String responseId = UUID.randomUUID().toString();
            // make a new record in the giftcode_redemption table
            GiftCodeRedemptionModel giftCodeRedemption = new GiftCodeRedemptionModel();
            giftCodeRedemption.setCloudNameCreated(dependentCloudName);
            giftCodeRedemption.setGiftCodeId(giftCodes[i]);
            giftCodeRedemption.setRedemptionId(responseId);
            giftCodeRedemption.setTimeCreated(new Date());
            try
            {
               dao.getGiftCodeRedemptionDAO().insert(giftCodeRedemption);

            } catch (DAOException e)
            {
               logger.error("Error in updating giftcode redemption information "
                     + e.getMessage());
               errors = true;
            }
         }
         if (errors)
         {
            return null;
         }
         i++;
      }
      if (i < arrDependentCloudName.size())
      {
         mv = new ModelAndView("creditCardPayment");
         /*
          * CSPModel cspModel = null;
          * 
          * try { cspModel = DAOFactory.getInstance().getCSPDAO()
          * .get(this.getCspCloudName()); } catch (DAOException e) { // TODO
          * Auto-generated catch block e.printStackTrace(); }
          * 
          * PaymentForm paymentForm = new PaymentForm();
          * paymentForm.setTxnType(PaymentForm.TXN_TYPE_DEP);
          * paymentForm.setNumberOfClouds(arrDependentCloudName.length - i);
          * mv.addObject("paymentInfo", paymentForm);
          * 
          * BigDecimal amount = cspModel.getCostPerCloudName().multiply( new
          * BigDecimal(arrDependentCloudName.length - i));
          * 
          * String desc = "Personal cloud  " + regSession.getCloudName();
          * mv.addObject("cspModel", cspModel); if
          * (cspModel.getPaymentGatewayName().equals("STRIPE")) {
          * logger.debug("Payment gateway is STRIPE");
          * mv.addObject("StripeJavaScript",
          * StripePaymentProcessor.getJavaScript(cspModel, amount, desc)); }
          * else if (cspModel.getPaymentGatewayName().equals("SAGEPAY")) {
          * 
          * mv.addObject("postURL", request.getContextPath()
          * +"/submitCustomerDetail"); mv.addObject("SagePay","SAGEPAY");
          * mv.addObject("amount",amount.toPlainString()); } else if
          * (cspModel.getPaymentGatewayName().equals("BRAINTREE")) {
          * logger.debug("Payment gateway is BRAINTREE");
          * mv.addObject("BrainTree" ,
          * BrainTreePaymentProcessor.getJavaScript(cspModel));
          * mv.addObject("postURL", request.getContextPath() + "/ccpayment");
          * mv.addObject("amount",amount.toPlainString()); }
          * mv.addObject("paymentInfo", paymentForm);
          */
         return mv;

      }
      regSession.setDependentForm(null);
      mv = new ModelAndView("dependentDone");

      return mv;
   }

   /**
    * Method to get the registered email address of signed in cloudname.
    * @return emailAddress.
    */
   public String getRegisteredEmailAddress() {
       String emailAddress = null;
       SignupInfoModel signupInfo = getSignupInfo();
       if (signupInfo != null) {
           emailAddress = signupInfo.getEmail();
       }
       return emailAddress;
   }

   /**
    * Method to get the registered phone number of signed in cloudname.
    * @return phoneNumber.
    */
   public String getRegisteredPhoneNumber() {
       String phoneNumber = null;
       SignupInfoModel signupInfo = getSignupInfo();
       if (signupInfo != null) {
           phoneNumber = signupInfo.getPhone();
       }
       return phoneNumber;
   }

   /**
    * Method to get logged in user's signup info.
    */
   private SignupInfoModel getSignupInfo() {
       SignupInfoModel signupInfo = null;
       SignupInfoDAO signupInfoDAO = DAOFactory.getInstance().getSignupInfoDAO();
       try
       {
          signupInfo = signupInfoDAO.get(regSession.getCloudName());
       } catch (DAOException e)
       {
          logger.error("Error getting signupInfo: " + e.getMessage());
       }
       return signupInfo;
   }

   private boolean registerCloudName(String cloudName, String phone,
         String email, String password, String paymentType, String paymentRefId, Locale locale)
   {
      try
      {
         registrationManager.registerUser(CloudName.create(cloudName), phone,
               email, password, null, paymentType, paymentRefId, locale);

         logger.debug("Sucessfully Registered {}", cloudName);

         return true;
      } catch (Xdi2ClientException e1)
      {

         logger.debug("Xdi2ClientException in registering cloud "
               + e1.getMessage());
      } catch (CSPRegistrationException e1)
      {

         logger.debug("CSPRegistrationException in registering cloud "
               + e1.getMessage());
      }
      return false;

   }

   private ModelAndView createGiftCards(HttpServletRequest request,
         String cloudName, PaymentModel payment, CSPModel cspModel)
   {
      ModelAndView mv = getCloudPage(request, cloudName);
      boolean errors = false;
      String errorText = "";

      InviteForm inviteForm = regSession.getInviteForm();
      if (inviteForm == null)
      {
         logger.debug("createGiftCards :: inviteForm is null!");
      }

      InviteModel inviteModel = null;
      try
      {
         /*
          * inviteModel =
          * DAOFactory.getInstance().getInviteDAO().get(inviteForm.
          * getInviteId()); if( inviteModel != null ) {
          * logger.error("InviteModel already exist - " + inviteModel); errors =
          * true; errorText = "Invite id has already been used before !"; }
          */
         List<GiftCodeModel> giftCardList = new ArrayList<GiftCodeModel>();

         InviteModel invite = PersonalCloudInviteController.saveInvite(
               inviteForm, payment, giftCardList, request.getLocale(),
               cspModel.getCspCloudName(), cloudName, request);
         mv = new ModelAndView("inviteDone");
         mv.addObject("cspModel", cspModel);
         mv.addObject("inviteModel", invite);
         mv.addObject("giftCardList", giftCardList);

      } catch (DAOException e)
      {
         logger.debug(e.getMessage());
         errors = true;
         errorText = "System error";
      }
      regSession.setInviteForm(null);

      if (errors)
      {
         mv.addObject("error", errorText);
      }
      return mv;
   }

   @RequestMapping(value = "/submitCustomerDetail", method = RequestMethod.POST)
   public ModelAndView processBillingDetail(

   HttpServletRequest request, HttpServletResponse response)
   {
      CSPModel cspModel = null;

      try
      {
         cspModel = DAOFactory.getInstance().getCSPDAO()
               .get(this.getCspCloudName());
      } catch (DAOException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      ModelAndView mv = new ModelAndView("submitCustomerDetail");
      mv.addObject("postURL", cspModel.getPaymentUrlTemplate());
      mv.addObject("SagePay", "SAGEPAY");
      mv.addObject("vendor", cspModel.getUsername());
      mv.addObject("crypt", SagePayPaymentProcessor.getSagePayCrypt(request,
            new BigDecimal(request.getParameter("amount")),
            cspModel.getCurrency(), cspModel.getPassword()));
      return mv;
   }

   @RequestMapping(value = "/transactionSuccessFailure", method = RequestMethod.POST)
   public ModelAndView showTransactionSuccessFailureForm(
         HttpServletRequest request, Model model)
   {
      logger.info("showing transactionSuccessFailure form "
            + request.getParameter("nextHop") + "::"
            + request.getParameter("cloudname"));

      if (request.getParameter("statusText") != null
            && !request.getParameter("statusText").contains("Sorry"))
      {
         // the transaction has gone through fine. So, post latitutide-longitude
         try
         {
            this.postLatitudeLongitudeInfo();
         } catch(Exception ex)
         {
            logger.debug("Could not post latitude-longitude information to RN " + ex.getMessage());
         }

      }
      ModelAndView mv = null;
      mv = new ModelAndView("postTxn");
      String formPostURL = request.getParameter("nextHop");
      mv.addObject("postURL", formPostURL);
      mv.addObject("cloudName", request.getParameter("cloudname"));
      mv.addObject("statusText", request.getParameter("statusText"));
      mv.addObject("submitMethod", request.getParameter("submitMethod"));
      mv.addObject("queryStr", request.getParameter("queryStr"));
      this.clearPaymentInfo();
      return mv;
   }

   public boolean postLatitudeLongitudeInfo() throws Exception
   {
      String latLongPostURL = RegistrationManager.getLatLongPostURL();
      logger.debug("Going to post latitude-longitude to RN " + latLongPostURL);
      CloseableHttpClient httpclient = HttpClients.createDefault();
      try
      {
         HttpPost httpPost = new HttpPost(latLongPostURL);
         List<NameValuePair> nvps = new ArrayList<NameValuePair>();
         String payload = "{ newmemberlocations : "
               + "["
               + "{ \"latitude\" : " + regSession.getLatitude() + ", \"longitude\" :" +  regSession.getLongitude() +  "}"
               
               + "] }";

         if(regSession != null)
         {
            nvps.add(new BasicNameValuePair("newmemberlocations", payload));
         }
         httpPost.setEntity(new UrlEncodedFormEntity(nvps));
         CloseableHttpResponse response2 = httpclient.execute(httpPost);

         try
         {
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity2);
         } finally
         {
            response2.close();
         }

      } finally
      {
         httpclient.close();
      }
      return true;
   }
   public  String getRNpostRegistrationLandingPage()
   {
      return RegistrationManager.getPostRegistrationURL();
      
   }
   public void clearSession()
   {
      if(regSession != null)
      {
      regSession.setCloudName(null);
      regSession.setPassword(null);
      regSession.setVerifiedEmail(null);
      regSession.setDependentForm(null);
      regSession.setGiftCode(null);
      regSession.setInviteCode(null);
      regSession.setInviteForm(null);
      regSession.setSessionId(null);
      regSession.setVerifiedMobilePhone(null);
      }
   }
   public void clearPaymentInfo()
   {
      if(regSession != null)
      {
      
         regSession.setGiftCode(null);
      }
   }
   public String formatQueryStr(String cloudName , String rnQueryString , HttpServletRequest request)
   {
      String queryStr = "";;
      try
      {
         queryStr = "name="
               + URLEncoder.encode(cloudName, "UTF-8");
         if(regSession.getRnQueryString().indexOf("csp=") < 0)
         {
            queryStr += "&csp="
                        + URLEncoder.encode(request.getContextPath()
                        .replace("/", "+"), "UTF-8");
         }
         queryStr += regSession.getRnQueryString();
      } catch (UnsupportedEncodingException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return queryStr;
   }

}
