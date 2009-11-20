/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */
package org.restlet.ext.jaxrs;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.ApplicationConfig;
import javax.ws.rs.core.MediaType;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Directory;
import org.restlet.Finder;
import org.restlet.Guard;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.Router;
import org.restlet.data.Language;
import org.restlet.ext.jaxrs.internal.todo.NotYetImplementedException;
import org.restlet.ext.jaxrs.internal.util.Converter;
import org.restlet.ext.jaxrs.internal.util.HtmlPreferer;
import org.restlet.service.MetadataService;
import org.restlet.service.TunnelService;

/**
 * <p>
 * This is the main class to be used for the instantiation of a JAX-RS runtime
 * environment.
 * </p>
 * <p>
 * To set up a JAX-RS runtime environment you should instantiate a
 * {@link JaxRsApplication}.
 * <ul>
 * <li>Attach your {@link ApplicationConfig}(s) by calling
 * {@link #attach(ApplicationConfig)}.</li>
 * <li> If you need authentication, set a {@link Guard} and perhaps an
 * {@link RoleChecker}, see {@link #setGuard(Guard)} or
 * {@link #setAuthentication(Guard, RoleChecker)}.</li>
 * <li>If you do not need preferation of HTML before XML, switch it off by
 * calling {@link #setPreferHtml(boolean) #setUseHtmlPreferer(false)}.</li>
 * </ul>
 * At least add the JaxRsApplication to a {@link Component}.
 * </p>
 * <p>
 * <i>The JAX-RS extension as well as the JAX-RS specification are currently
 * under development. You should use this extension only for experimental
 * purpose.</i> <br>
 * For further information see <a href="https://jsr311.dev.java.net/">Java
 * Service Request 311</a>.
 * </p>
 * 
 * @author Stephan Koops
 */
public class JaxRsApplication extends Application {

    /** the {@link Guard} to use. May be null. */
    private volatile Guard guard;

    /** The {@link JaxRsRouter} to use. */
    private volatile JaxRsRouter jaxRsRouter;

    /** Indicates, if an {@link HtmlPreferer} should be used or not. */
    private volatile boolean preferHtml = true;

    /**
     * Default constructor.
     * 
     * @param parentContext
     *                The parent component context.
     */
    public JaxRsApplication(Context parentContext) {
        super(parentContext);
        this.jaxRsRouter = new JaxRsRouter(parentContext);
    }

    /**
     * Adds the extension mappings for mediat types and languages, given by the
     * {@link ApplicationConfig} to the {@link TunnelService}.
     * 
     * @param appConfig
     *                the ApplicationConfig to read the mappings from.
     */
    private void addExtensionMappings(ApplicationConfig appConfig) {
        MetadataService metadataService = this.getMetadataService();
        Map<String, MediaType> mediaTypeMapping = appConfig
                .getExtensionMappings();
        if (mediaTypeMapping != null) {
            for (Map.Entry<String, MediaType> e : mediaTypeMapping.entrySet()) {
                org.restlet.data.MediaType restletMediaType;
                restletMediaType = Converter.toRestletMediaType(e.getValue());
                metadataService.addExtension(e.getKey(), restletMediaType);
            }
        }
        Map<String, String> languageMapping = appConfig.getLanguageMappings();
        if (mediaTypeMapping != null) {
            for (Map.Entry<String, String> e : languageMapping.entrySet()) {
                Language language = Language.valueOf(e.getValue());
                metadataService.addExtension(e.getKey(), language);
            }
        }
    }

    /**
     * <p>
     * Attaches an {@link ApplicationConfig} to this Application.<br>
     * The providers are available for all root resource classes provided to
     * this JaxRsRouter. If you won't mix them, instantiate another JaxRsRouter.
     * </p>
     * <p>
     * If the given ApplicationConfig is the first attached ApplicationConfig
     * (more exactly: if no root resource classes are available), the default
     * extension mappings are remove an replaced by the given, see
     * {@link TunnelService}.
     * </p>
     * 
     * @param appConfig
     *                Contains the classes to load as root resource classes and
     *                as providers.
     * @throws IllegalArgumentException
     *                 if {@link ApplicationConfig} contains non-valid resource
     *                 classes or non-valid providers, or one of their
     *                 constructors throws an exception.
     * @throws NullPointerException
     *                 if the appConfig is null.
     * 
     * @see #attach(ApplicationConfig, boolean)
     */
    public void attach(ApplicationConfig appConfig)
            throws IllegalArgumentException {
        attach(appConfig, true);
    }

    /**
     * Attaches an {@link ApplicationConfig} to this Application.<br>
     * The providers are available for all root resource classes provided to
     * this JaxRsRouter. If you won't mix them, instantiate another JaxRsRouter.
     * 
     * @param appConfig
     *                Contains the classes to load as root resource classes and
     *                as providers.
     * @param clearMetadataIfFirst
     *                If this flag is true and the given ApplicationConfig is
     *                the first attached ApplicationConfig (more exactly: if no
     *                root resource classes are available), the default
     *                extension mappings are remove an replaced by the new, see
     *                {@link TunnelService}
     * @throws IllegalArgumentException
     *                 if {@link ApplicationConfig} contains non-valid resource
     *                 classes or non-valid providers, or one of their
     *                 constructors throws an exception.
     * @throws NullPointerException
     *                 if the appConfig is null.
     * @see #attach(ApplicationConfig)
     */
    public void attach(ApplicationConfig appConfig, boolean clearMetadataIfFirst)
            throws IllegalArgumentException {
        if (clearMetadataIfFirst && this.jaxRsRouter.isEmpty()) {
            // this.getMetadataService().clearExtensions();
            // wait for a patch
        }
        this.addExtensionMappings(appConfig);
        this.jaxRsRouter.attach(appConfig);
    }

    @Override
    public Restlet createRoot() {

        Restlet restlet = jaxRsRouter;

        if (this.guard != null) {
            this.guard.setNext(restlet);
            restlet = this.guard;
        }

        // some browser request XML with higher quality than HTML.
        // If you want to change the quality, use this HtmlPreferer
        // filter. If you do not need it, you can directly return the
        // router.
        restlet = new HtmlPreferer(getContext(), restlet);

        return restlet;
    }

    /**
     * Returns the Guard
     * 
     * @return the Guard
     */
    public Guard getGuard() {
        return this.guard;
    }

    /**
     * Returns the current RoleChecker
     * 
     * @return the current RoleChecker
     */
    public RoleChecker getRoleChecker() {
        return this.jaxRsRouter.getRoleChecker();
    }

    /**
     * Returns an unmodifiable set with the attached root resource classes.
     * 
     * @return an unmodifiable set with the attached root resource classes.
     */
    public Collection<Class<?>> getRootResources() {
        return this.jaxRsRouter.getRootResourceClasses();
    }

    /**
     * Returns an unmodifiable set of supported URIs (relative to this
     * Application).
     * 
     * @return an unmodifiable set of supported URIs (relative).
     */
    public Collection<String> getRootUris() {
        return this.jaxRsRouter.getRootUris();
    }

    /**
     * <i>This method is planned!</i><br>
     * It should return {@link Route}s to attach them to a {@link Router}.<br>
     * The {@link JaxRsRouter} does not allow other Restlets directly beside it.
     * Example: {@link JaxRsRouter} handles http://host/path1. So you can't
     * directly add another Restlet handling http://host/path2. When addings
     * this {@link Route}s to the main {@link Router} for "host" you can add
     * another {@link Restlet} (e.g. a {@link Directory} or {@link Finder}) for
     * other pathes.
     * 
     * @return an unmodifiable {@link List} of {@link Route}s.
     * @deprecated planned, but not yet implemented
     */
    @Deprecated
    @SuppressWarnings("unused")
    private List<Route> getRoutes() {
        throw new NotYetImplementedException();
    }

    /**
     * Returns the state, if a {@link HtmlPreferer} should be used or not.<br>
     * The default value is true for now, but may change later.
     * 
     * @return the state, if a {@link HtmlPreferer} should be used or not.
     * @see #setPreferHtml(boolean)
     */
    public boolean isPreferHtml() {
        return this.preferHtml;
    }

    /**
     * Sets the objects to check the authentication. The {@link Guard} checks
     * the username and password (e.g.), the {@link RoleChecker} manages the
     * role management for the JAX-RS extension.
     * 
     * @param guard
     *                the Guard to use.
     * @param roleChecker
     *                the RoleChecker to use
     * @see #setGuard(Guard)
     * @see #setRoleChecker(RoleChecker)
     */
    public void setAuthentication(Guard guard, RoleChecker roleChecker) {
        this.setGuard(guard);
        this.setRoleChecker(roleChecker);
    }

    /**
     * Sets the {@link Guard} to use.<br>
     * The new one is ignored, after the root Restlet is created (see
     * {@link #createRoot()}.
     * 
     * @param guard
     *                the Guard to use.
     * @see #setAuthentication(Guard, RoleChecker)
     */
    public void setGuard(Guard guard) {
        this.guard = guard;
    }

    /**
     * Some browsers (e.g. Internet Explorer 7.0 and Firefox 2.0) sends as
     * accepted media type XML with a higher quality than HTML. The consequence
     * is, that a HTTP server sends XML instead of HTML, if it could produce
     * XML. To avoid this, set this property to true, or false, if you want
     * default behaviour.<br>
     * This setting is ignored after creation of the root (see
     * {@link #createRoot()}.<br>
     * The default value is true for now, but may change later.
     * 
     * @param preferHtml
     *                if true, a {@link HtmlPreferer} is used, if false then
     *                not.
     * @see #isPreferHtml()
     * @see HtmlPreferer
     */
    public void setPreferHtml(boolean preferHtml) {
        this.preferHtml = preferHtml;
    }

    /**
     * Sets the {@link RoleChecker} to use.<br>
     * If you give an RoleChecker, you should also give a Guard.
     * 
     * @param roleChecker
     * @see #setAuthentication(Guard, RoleChecker)
     * @see #setGuard(Guard)
     */
    public void setRoleChecker(RoleChecker roleChecker) {
        this.jaxRsRouter.setRoleChecker(roleChecker);
    }
}