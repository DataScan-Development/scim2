package com.bettercloud.scim2.server.controller.discovery;

import com.bettercloud.scim2.common.exceptions.ResourceNotFoundException;
import com.bettercloud.scim2.common.utils.ApiConstants;
import com.bettercloud.scim2.server.ResourceTypeDefinition;
import com.bettercloud.scim2.server.config.Scim2Properties;
import com.bettercloud.scim2.server.controller.BaseResourceController;
import com.bettercloud.scim2.server.evaluator.SchemaAwareFilterEvaluator;
import com.google.common.base.Throwables;
import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.ScimResource;
import com.bettercloud.scim2.common.exceptions.ScimException;
import com.bettercloud.scim2.common.filters.Filter;
import com.bettercloud.scim2.common.messages.ListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SchemaAwareController extends BaseResourceController<GenericScimResource> {

    private List<GenericScimResource> resources;

    private final SchemaAwareFilterEvaluator filterEvaluator = new SchemaAwareFilterEvaluator(resourceTypeDefinition);

    protected abstract List<GenericScimResource> getResources(final Set<ResourceTypeDefinition> resourceDefinitions);

    protected Set<ResourceTypeDefinition> resourceDefinitions;

    @Autowired
    public SchemaAwareController(final Scim2Properties scim2Properties,
                                 final Set<ResourceTypeDefinition> resourceDefinitions) {
        super(scim2Properties);
        this.resourceDefinitions = resourceDefinitions;
        resources = getResources(resourceDefinitions);
    }

    /**
     * Service SCIM request to retrieve all resource types or schemas defined at the
     * service provider using GET.
     *
     * @return All resource types in a ListResponse container.
     *
     * @throws ScimException If an error occurs.
     */
    @GetMapping
    public ListResponse<GenericScimResource> search() throws ScimException {

        final List<GenericScimResource> preparedResources = genericScimResourceConverter.convert(null, null, resources);

        return new ListResponse<>(preparedResources.size(), preparedResources, 1, preparedResources.size());
    }

    /**
     * Service SCIM request to retrieve a resource type or schema by ID.
     *
     * @param id The ID of the resource type to retrieve.
     *
     * @return The retrieved resource type.
     *
     * @throws ScimException If an error occurs.
     */
    @GetMapping(value = "/{id}")
    public ScimResource get(@PathVariable("id") final String id) throws ScimException {
        final Filter filter = Filter.eq("id", id);
        return filterResources(filter).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private List<GenericScimResource> filterResources(final Filter filter) {
        return resources.stream().filter(genericScimResource -> {
            try {
                return filter.visit(filterEvaluator, genericScimResource.getObjectNode());
            } catch (ScimException e) {
                throw Throwables.propagate(e);
            }
        }).collect(Collectors.toList());
    }
}