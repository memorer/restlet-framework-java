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

package org.restlet.resource;

import java.util.ArrayList;
import java.util.List;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Handler;
import org.restlet.data.Dimension;
import org.restlet.data.Language;
import org.restlet.data.Parameter;
import org.restlet.data.ReferenceList;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.util.Series;

/**
 * Intended conceptual target of a hypertext reference. "Any information that
 * can be named can be a resource: a document or image, a temporal service (e.g.
 * "today's weather in Los Angeles"), a collection of other resources, a
 * non-virtual object (e.g. a person), and so on. In other words, any concept
 * that might be the target of an author's hypertext reference must fit within
 * the definition of a resource. The only thing that is required to be static
 * for a resource is the semantics of the mapping, since the semantics is what
 * distinguishes one resource from another." Roy T. Fielding<br>
 * <br>
 * Another definition adapted from the URI standard (RFC 3986): a resource is
 * the conceptual mapping to a representation (also known as entity) or set of
 * representations, not necessarily the representation which corresponds to that
 * mapping at any particular instance in time. Thus, a resource can remain
 * constant even when its content (the representations to which it currently
 * corresponds) changes over time, provided that the conceptual mapping is not
 * changed in the process. In addition, a resource is always identified by a
 * URI.<br>
 * <br>
 * Typically created by Finders, Resource instances are the final handlers of
 * calls received by server connectors or by the internal router. Unlike the
 * other processors in the chain, a Resource is not shared between calls and
 * doesn't have to be thread-safe.<br>
 * <br>
 * This is the point where the RESTful view of your Web application can be
 * integrated with your domain objects. Those domain objects can be implemented
 * using any technology, relational databases, object databases, transactional
 * components like EJB, etc.<br>
 * <br>
 * You just have to extend this class to override the REST methods you want to
 * support like {@link #acceptRepresentation(Representation)} for POST
 * processing, {@link #storeRepresentation(Representation)} for PUT processing
 * or {@link #removeRepresentations()} for DELETE processing.<br>
 * <br>
 * The common GET method is supported by the modifiable "variants" list property
 * and the {@link #represent(Variant)} method. This allows an easy and cheap
 * declaration of the available variants, in the constructor for example. Then
 * the creation of costly representations is delegated to the
 * {@link #represent(Variant)} method when actually needed.<br>
 * <br>
 * 
 * @see <a
 *      href="http://roy.gbiv.com/pubs/dissertation/rest_arch_style.htm#sec_5_2_1_1">Source
 *      dissertation</a>
 * @see <a
 *      href="http://www.restlet.org/documentation/1.1/tutorial#part12">Tutorial:
 *      Reaching target Resources</a>
 * @see org.restlet.resource.Representation
 * @see org.restlet.Finder
 * @author Jerome Louvel (contact@noelios.com)
 * @author Thierry Boileau (thboileau@gmail.com)
 * @author Konstantin Laufer (laufer@cs.luc.edu)
 */
public class Resource extends Handler {

    /** Indicates if the resource is actually available. */
    private boolean available;

    /**
     * Indicates if the representations can be modified via the
     * {@link #handlePost()}, the {@link #handlePut()} or the
     * {@link #handleDelete()} methods.
     */
    private boolean modifiable;

    /** Indicates if the best content is automatically negotiated. */
    private boolean negotiateContent;

    /**
     * Indicates if the representations can be read via the {@link #handleGet()}
     * method.
     */
    private boolean readable;

    /** The modifiable list of variants. */
    private volatile List<Variant> variants;

    /**
     * Initializer block to ensure that the basic properties of the Resource are
     * initialized consistently across constructors.
     */
    {
        this.available = true;
        this.modifiable = false;
        this.negotiateContent = true;
        this.readable = true;
        this.variants = null;
    }

    /**
     * Special constructor used by IoC frameworks. Note that the init() method
     * MUST be invoked right after the creation of the handler in order to keep
     * a behavior consistent with the normal three arguments constructor.
     */
    public Resource() {
    }

    /**
     * Normal constructor. This constructor will invoke the parent constructor
     * by default.
     * 
     * @param context
     *                The parent context.
     * @param request
     *                The request to handle.
     * @param response
     *                The response to return.
     */
    public Resource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    /**
     * Accepts and processes a representation posted to the resource. The
     * default behavior is to set the response status to
     * {@link Status#SERVER_ERROR_INTERNAL}.<br>
     * <br>
     * This is the higher-level method that let you process POST requests.
     * 
     * @param entity
     *                The posted entity.
     */
    @SuppressWarnings("unused")
    public void acceptRepresentation(Representation entity)
            throws ResourceException {
        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
    }

    /**
     * Indicates if DELETE calls are allowed by checking the "modifiable"
     * property.
     * 
     * @return True if the method is allowed.
     */
    @Override
    public boolean allowDelete() {
        return isModifiable();
    }

    /**
     * Indicates if GET calls are allowed by checking the "readable" property.
     * 
     * @return True if the method is allowed.
     */
    @Override
    public boolean allowGet() {
        return isReadable();
    }

    /**
     * Indicates if POST calls are allowed by checking the "modifiable"
     * property.
     * 
     * @return True if the method is allowed.
     */
    @Override
    public boolean allowPost() {
        return isModifiable();
    }

    /**
     * Indicates if DELETE calls are allowed by checking the "modifiable"
     * property.
     * 
     * @return True if the method is allowed.
     */
    @Override
    public boolean allowPut() {
        return isModifiable();
    }

    /**
     * Asks the resource to delete itself and all its representations.The
     * default behavior is to invoke the {@link #removeRepresentations()}
     * method.
     * 
     * @deprecated Use the {@link #removeRepresentations()} method instead.
     */
    @Deprecated
    public void delete() {
        try {
            removeRepresentations();
        } catch (ResourceException re) {
            getResponse().setStatus(re.getStatus());
        }
    }

    /**
     * Returns the preferred representation according to the client preferences
     * specified in the request.
     * 
     * @return The preferred representation.
     * @deprecated Use the {@link #represent()} method instead.
     * @see #getPreferredVariant()
     */
    @Deprecated
    public Representation getPreferredRepresentation() {
        Representation result = null;

        try {
            result = represent();
        } catch (ResourceException re) {
            getResponse().setStatus(re.getStatus());
        }

        return result;
    }

    /**
     * Returns the preferred variant according to the client preferences
     * specified in the request.
     * 
     * @return The preferred variant.
     */
    public Variant getPreferredVariant() {
        Variant result = null;
        List<Variant> variants = getVariants();

        if ((variants != null) && (!variants.isEmpty())) {
            Language language = null;
            // Compute the preferred variant. Get the default language
            // preference from the Application (if any).
            Application app = Application.getCurrent();

            if (app != null) {
                language = app.getMetadataService().getDefaultLanguage();
            }

            result = getRequest().getClientInfo().getPreferredVariant(variants,
                    language);

        }

        return result;
    }

    /**
     * Returns a full representation for a given variant previously returned via
     * the getVariants() method. The default implementation directly returns the
     * variant in case the variants are already full representations. In all
     * other cases, you will need to override this method in order to provide
     * your own implementation. <br>
     * <br>
     * 
     * This method is very useful for content negotiation when it is too costly
     * to initilize all the potential representations. It allows a resource to
     * simply expose the available variants via the getVariants() method and to
     * actually server the one selected via this method.
     * 
     * @param variant
     *                The variant whose full representation must be returned.
     * @return The full representation for the variant.
     * @see #getVariants()
     * @deprecated Use the {@link #represent(Variant)} method instead.
     */
    @Deprecated
    public Representation getRepresentation(Variant variant) {
        Representation result = null;

        try {
            result = represent(variant);
        } catch (ResourceException re) {
            getResponse().setStatus(re.getStatus());
        }

        return result;
    }

    /**
     * Returns the modifiable list of variants. Creates a new instance if no one
     * has been set. A variant can be a purely descriptive representation, with
     * no actual content that can be served. It can also be a full
     * representation in case a resource has only one variant or if the
     * initialization cost is very low.<br>
     * <br>
     * Note that the order in which the variants are inserted in the list
     * matters. For example, if the client has no preference defined, or if the
     * acceptable variants have the same quality level for the client, the first
     * acceptable variant in the list will be returned.<br>
     * <br>
     * It is recommended to not override this method and to simply use it at
     * construction time to initialize the list of available variants.
     * Overriding it may reconstruct the list for each call which can be
     * expensive.
     * 
     * @return The list of variants.
     * @see #getRepresentation(Variant)
     */
    public List<Variant> getVariants() {
        // Lazy initialization with double-check.
        List<Variant> v = this.variants;
        if (v == null) {
            synchronized (this) {
                v = this.variants;
                if (v == null)
                    this.variants = v = new ArrayList<Variant>();
            }
        }
        return v;
    }

    /**
     * Handles a DELETE call by invoking the {@link #removeRepresentations()}
     * method. It also automatically support conditional DELETEs.
     */
    @Override
    public void handleDelete() {
        boolean canDelete = true;
        if (getRequest().getConditions().hasSome()) {
            Variant preferredVariant = null;

            if (isNegotiateContent()) {
                preferredVariant = getPreferredVariant();
            } else {
                List<Variant> variants = getVariants();

                if (variants.size() == 1) {
                    preferredVariant = variants.get(0);
                } else {
                    getResponse().setStatus(
                            Status.CLIENT_ERROR_PRECONDITION_FAILED);
                    canDelete = false;
                }
            }

            // The conditions have to be checked
            // even if there is no preferred variant.
            if (canDelete) {
                Status status = getRequest().getConditions().getStatus(
                        getRequest().getMethod(),
                        getRepresentation(preferredVariant));

                if (status != null) {
                    getResponse().setStatus(status);
                    canDelete = false;
                }
            }
        }

        if (canDelete) {
            delete();
        }
    }

    /**
     * Handles a GET call by automatically returning the best representation
     * available. The content negotiation is automatically supported based on
     * the client's preferences available in the request. This feature can be
     * turned off using the "negotiateContent" property.<br>
     * <br>
     * If the resource's "available" property is set to false, the method
     * immediately returns with a {@link Status#CLIENT_ERROR_NOT_FOUND} status.<br>
     * <br>
     * The negotiated representation is obtained by calling the
     * {@link #getPreferredVariant()}. If a variant is sucessfully selected,
     * then the {@link #getRepresentation(Variant)} method is called to get the
     * actual representation corresponding to the metadata in the variant.<br>
     * <br>
     * If no variant matching the client preferences is available, the response
     * status is set to {@link Status#CLIENT_ERROR_NOT_ACCEPTABLE} and the list
     * of available representations is returned in the response entity as a
     * textual list of URIs (only if the variants have an identifier properly
     * set).<br>
     * <br>
     * If the content negotiation is turned off and only one variant is defined
     * in the "variants" property, then its representation is returned by
     * calling the {@link #getRepresentation(Variant)} method. If several
     * variants are available, then the list of available representations is
     * returned in the response entity as a textual list of URIs (only if the
     * variants have an identifier properly set).<br>
     * <br>
     * If no variant is defined in the "variants" property, the response status
     * is set to {@link Status#CLIENT_ERROR_NOT_FOUND}. <br>
     * If it is disabled and multiple variants are available for the target
     * resource, then a 300 (Multiple Choices) status will be returned with the
     * list of variants URI if available. Conditional GETs are also
     * automatically supported.
     */
    @Override
    public void handleGet() {
        if (!isAvailable()) {
            // Resource not existing or not available to the current client
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            // The variant that may need to meet the request conditions
            Representation selectedRepresentation = null;

            List<Variant> variants = getVariants();
            if ((variants == null) || (variants.isEmpty())) {
                // Resource not found
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getLogger()
                        .warning(
                                "A resource should normally have at least one variant added by calling getVariants().add() in the constructor. Check your resource \""
                                        + getRequest().getResourceRef() + "\".");
            } else if (isNegotiateContent()) {
                Variant preferredVariant = getPreferredVariant();

                if (preferredVariant == null) {
                    // No variant was found matching the client preferences
                    getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);

                    // The list of all variants is transmitted to the client
                    ReferenceList refs = new ReferenceList(variants.size());
                    for (Variant variant : variants) {
                        if (variant.getIdentifier() != null) {
                            refs.add(variant.getIdentifier());
                        }
                    }

                    getResponse().setEntity(refs.getTextRepresentation());
                } else {
                    // Set the variant dimensions used for content negotiation
                    getResponse().getDimensions().clear();
                    getResponse().getDimensions().add(Dimension.CHARACTER_SET);
                    getResponse().getDimensions().add(Dimension.ENCODING);
                    getResponse().getDimensions().add(Dimension.LANGUAGE);
                    getResponse().getDimensions().add(Dimension.MEDIA_TYPE);

                    // Set the negotiated representation as response entity
                    getResponse()
                            .setEntity(getRepresentation(preferredVariant));
                }

                selectedRepresentation = getResponse().getEntity();
            } else {
                if (variants.size() == 1) {
                    getResponse().setEntity(getRepresentation(variants.get(0)));
                    selectedRepresentation = getResponse().getEntity();
                } else {
                    ReferenceList variantRefs = new ReferenceList();

                    for (Variant variant : variants) {
                        if (variant.getIdentifier() != null) {
                            variantRefs.add(variant.getIdentifier());
                        } else {
                            getLogger()
                                    .warning(
                                            "A resource with multiple variants should provide an identifier for each variant when content negotiation is turned off");
                        }
                    }

                    if (variantRefs.size() > 0) {
                        // Return the list of variants
                        getResponse().setStatus(
                                Status.REDIRECTION_MULTIPLE_CHOICES);
                        getResponse().setEntity(
                                variantRefs.getTextRepresentation());
                    } else {
                        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                    }
                }
            }

            if (selectedRepresentation == null) {
                if ((getResponse().getStatus() == null)
                        || getResponse().getStatus().isSuccess()) {
                    getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                } else {
                    // Keep the current status as the developer might prefer a
                    // special status like 'method not authorized'.
                }
            } else {
                // The given representation (even if null) must meet the request
                // conditions (if any).
                if (getRequest().getConditions().hasSome()) {
                    Status status = getRequest().getConditions().getStatus(
                            getRequest().getMethod(), selectedRepresentation);

                    if (status != null) {
                        getResponse().setStatus(status);
                        getResponse().setEntity(null);
                    }
                }
            }
        }
    }

    /**
     * Handles a POST call by invoking the
     * {@link #acceptRepresentation(Representation)} method. It also logs a
     * trace if there is no entity posted.
     */
    @Override
    public void handlePost() {
        if (!getRequest().isEntityAvailable()) {
            getLogger()
                    .fine(
                            "POST request received without any entity. Continuing processing.");
        }

        post(getRequest().getEntity());
    }

    /**
     * Handles a PUT call by invoking the
     * {@link #storeRepresentation(Representation)} method. It also handles
     * conditional PUTs and forbids partial PUTs as they are not supported yet.
     * Finally, it prevents PUT with no entity by setting the response status to
     * {@link Status#CLIENT_ERROR_BAD_REQUEST} following the HTTP
     * specifications.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void handlePut() {
        boolean canPut = true;

        if (getRequest().getConditions().hasSome()) {
            Variant preferredVariant = null;

            if (isNegotiateContent()) {
                preferredVariant = getPreferredVariant();
            } else {
                List<Variant> variants = getVariants();

                if (variants.size() == 1) {
                    preferredVariant = variants.get(0);
                } else {
                    getResponse().setStatus(
                            Status.CLIENT_ERROR_PRECONDITION_FAILED);
                    canPut = false;
                }
            }

            // The conditions have to be checked
            // even if there is no preferred variant.
            if (canPut) {
                Status status = getRequest().getConditions().getStatus(
                        getRequest().getMethod(),
                        getRepresentation(preferredVariant));
                if (status != null) {
                    getResponse().setStatus(status);
                    canPut = false;
                }
            }
        }

        if (canPut) {
            // Check the Content-Range HTTP Header
            // in order to prevent usage of partial PUTs
            Object oHeaders = getRequest().getAttributes().get(
                    "org.restlet.http.headers");
            if (oHeaders != null) {
                Series<Parameter> headers = (Series<Parameter>) oHeaders;
                if (headers.getFirst("Content-Range", true) != null) {
                    getResponse()
                            .setStatus(
                                    new Status(
                                            Status.SERVER_ERROR_NOT_IMPLEMENTED,
                                            "The Content-Range header is not understood"));
                    canPut = false;
                }
            }
        }

        if (canPut) {
            if (getRequest().isEntityAvailable()) {
                put(getRequest().getEntity());

                // HTTP spec says that PUT may return
                // the list of allowed methods
                updateAllowedMethods();
            } else {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Missing request entity");
            }
        }
    }

    /**
     * Initialize the resource with its context. If you override this method,
     * make sure that you don't forget to call super.init() first, otherwise
     * your Resource won't behave properly.
     * 
     * @param context
     *                The parent context.
     * @param request
     *                The request to handle.
     * @param response
     *                The response to return.
     */
    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
    }

    /**
     * Indicates if the resource is actually available. By default this property
     * is true but it can be set to false if the resource doesn't exist at all
     * or if it isn't visible to a specific client. The {@link #handleGet()}
     * method will set the response's status to
     * {@link Status#CLIENT_ERROR_NOT_FOUND} if this property is false.
     * 
     * @return True if the resource is available.
     */
    public boolean isAvailable() {
        return this.available;
    }

    /**
     * Indicates if the representations can be modified via the
     * {@link #handlePost()}, the {@link #handlePut()} or the
     * {@link #handleDelete()} methods.
     * 
     * @return True if representations can be modified.
     */
    public boolean isModifiable() {
        return this.modifiable;
    }

    /**
     * Indicates if the best content is automatically negotiated. Default value
     * is true.
     * 
     * @return True if the best content is automatically negotiated.
     */
    public boolean isNegotiateContent() {
        return this.negotiateContent;
    }

    /**
     * Indicates if the representations can be read via the {@link #handleGet()}
     * method.
     * 
     * @return True if the representations can be read.
     */
    public boolean isReadable() {
        return this.readable;
    }

    /**
     * Posts a representation to the resource. The default behavior is to invoke
     * the {@link #acceptRepresentation(Representation)} method.
     * 
     * @param entity
     *                The representation posted.
     * @deprecated Use the {@link #acceptRepresentation(Representation)} method
     *             instead.
     */
    @Deprecated
    public void post(Representation entity) {
        try {
            acceptRepresentation(entity);
        } catch (ResourceException re) {
            getResponse().setStatus(re.getStatus());
        }
    }

    /**
     * Puts a representation in the resource. The default behavior is to invoke
     * the {@link #storeRepresentation(Representation)} method.
     * 
     * @param entity
     *                The representation put.
     * @deprecated Use the {@link #storeRepresentation(Representation)} method
     *             instead.
     */
    @Deprecated
    public void put(Representation entity) {
        try {
            storeRepresentation(entity);
        } catch (ResourceException re) {
            getResponse().setStatus(re.getStatus());
        }
    }

    /**
     * Removes all the representations of the resource and effectively the
     * resource itself. The default behavior is to set the response status to
     * {@link Status#SERVER_ERROR_INTERNAL}.<br>
     * <br>
     * This is the higher-level method that let you process DELETE requests.
     */
    @SuppressWarnings("unused")
    public void removeRepresentations() throws ResourceException {
        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
    }

    /**
     * Returns the preferred representation according to the client preferences
     * specified in the request.
     * 
     * @return The preferred representation.
     * @see #getPreferredVariant()
     * @throws ResourceException
     */
    public Representation represent() throws ResourceException {
        return represent(getPreferredVariant());
    }

    /**
     * Returns a full representation for a given variant previously returned via
     * the getVariants() method. The default implementation directly returns the
     * variant in case the variants are already full representations. In all
     * other cases, you will need to override this method in order to provide
     * your own implementation. <br>
     * <br>
     * 
     * This method is very useful for content negotiation when it is too costly
     * to initialize all the potential representations. It allows a resource to
     * simply expose the available variants via the getVariants() method and to
     * actually server the one selected via this method.
     * 
     * @param variant
     *                The variant whose full representation must be returned.
     * @return The full representation for the variant.
     * @see #getVariants()
     */
    @SuppressWarnings("unused")
    public Representation represent(Variant variant) throws ResourceException {
        Representation result = null;

        if (variant instanceof Representation) {
            result = (Representation) variant;
        }

        return result;
    }

    /**
     * Indicates if the resource is actually available. By default this property
     * is true but it can be set to false if the resource doesn't exist at all
     * or if it isn't visible to a specific client. The {@link #handleGet()}
     * method will set the response's status to
     * {@link Status#CLIENT_ERROR_NOT_FOUND} if this property is false.
     * 
     * @param available
     *                True if the resource is actually available.
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * Indicates if the representations can be modified via the
     * {@link #handlePost()}, the {@link #handlePut()} or the
     * {@link #handleDelete()} methods.
     * 
     * @param modifiable
     *                Indicates if the representations can be modified.
     */
    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    /**
     * Indicates if the returned representation is automatically negotiated.
     * Default value is true.
     * 
     * @param negotiateContent
     *                True if content negotiation is enabled.
     */
    public void setNegotiateContent(boolean negotiateContent) {
        this.negotiateContent = negotiateContent;
    }

    /**
     * Indicates if the representations can be read via the {@link #handleGet()}
     * method.
     * 
     * @param readable
     *                Indicates if the representations can be read.
     */
    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    /**
     * Sets the modifiable list of variants.
     * 
     * @param variants
     *                The modifiable list of variants.
     */
    public void setVariants(List<Variant> variants) {
        this.variants = variants;
    }

    /**
     * Stores a representation put to the resource and replaces all existing
     * representations of the resource. If the resource doesn't exist yet, it
     * should create it and use the entity as its initial representation. The
     * default behavior is to set the response status to
     * {@link Status#SERVER_ERROR_INTERNAL}.<br>
     * <br>
     * This is the higher-level method that let you process PUT requests.
     * 
     * @param entity
     */
    @SuppressWarnings("unused")
    public void storeRepresentation(Representation entity)
            throws ResourceException {
        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
    }

}
