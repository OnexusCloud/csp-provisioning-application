package net.respectnetwork.csp.application.util;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.json.simple.JSONValue;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Locale;

public class ResponseBuilder {

    private final MessageSource messageSource;
    private final HttpServletRequest request;

    private Locale locale;
    private Multimap<String, String> errors;

    private ModelAndView mv = new ModelAndView();
    private String cloudName;

    public ResponseBuilder(MessageSource messageSource, HttpServletRequest request) {
        this.messageSource = messageSource;
        this.request = request;
    }

    public ModelAndView build() {
        return withModelView(mv);
    }

    public boolean hasErrors() {
        return (errors != null && !errors.isEmpty());
    }

    public boolean isEmpty() {
        return (errors == null || errors.isEmpty());
    }

    public ResponseBuilder addGeneralError(String msgKey, Object... msgParams) {
        addError("error", msgKey, msgParams);
        return this;
    }

    public ResponseBuilder addError(String formElementName, String msgKey, Object... msgParams) {
        String msg = msg(msgKey, msgParams);

        if (errors == null) {
            errors = ArrayListMultimap.create();
        }
        errors.put(formElementName, msg);
        return this;
    }

    public ModelAndView withModelView(ModelAndView mv) {
        if (errors != null && !errors.isEmpty()) {
            mv.addObject("errors", toJson());
        }
        return mv;
    }

    private String toJson() {
        if (errors == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append('{');
        for (String elementName : errors.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }

            // format and escape error messages
            Collection<String> msgs = errors.get(elementName);
            Iterable<String> msgsEsc = Iterables.transform(msgs, new Function<String, String>() {
                public String apply(String input) {
                    return (input == null) ? null : JSONValue.escape(HtmlUtils.htmlEscape(input));
                }
            });
            String errorMsg = Joiner.on("<br/>").join(msgsEsc);
            sb.append("'").append(elementName).append("':'").append(errorMsg).append("'");
        }
        sb.append('}');

        return sb.toString();
    }

    private String msg(String key, Object... params) {
        if (locale == null) {
            locale = request.getLocale();
        }
        return messageSource.getMessage(key, params, locale);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("messageSource", messageSource)
                .add("request", request)
                .add("locale", locale)
                .add("errors", errors)
                .toString();
    }

    public ResponseBuilder setView(String view) {
        mv.setViewName(view);
        return this;
    }

    public ResponseBuilder setCloudName(String cloudName) {
        this.cloudName = cloudName;
        addObject("cloudName", cloudName);
        return this;
    }

    public ResponseBuilder addObject(String name, Object value) {
        mv.addObject(name, value);
        return this;
    }
}
