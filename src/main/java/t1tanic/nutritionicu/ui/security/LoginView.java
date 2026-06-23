package t1tanic.nutritionicu.ui.security;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

/** Login screen. Posts to Spring Security's form-login endpoint; shows an error on bad credentials. */
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("login.title") + " · " + getTranslation("app.title");
    }

    private final LoginForm login = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        login.setI18n(loginI18n());

        Span hint = new Span(getTranslation("login.hint"));
        hint.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        add(new H1(getTranslation("app.title")), login, hint);
    }

    private LoginI18n loginI18n() {
        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form form = i18n.getForm();
        form.setTitle(getTranslation("login.title"));
        form.setUsername(getTranslation("login.username"));
        form.setPassword(getTranslation("login.password"));
        form.setSubmit(getTranslation("login.signin"));
        i18n.setForm(form);
        LoginI18n.ErrorMessage error = i18n.getErrorMessage();
        error.setTitle(getTranslation("login.error.title"));
        error.setMessage(getTranslation("login.error.message"));
        i18n.setErrorMessage(error);
        return i18n;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
