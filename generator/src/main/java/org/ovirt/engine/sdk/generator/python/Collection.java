//
// Copyright (c) 2014 Red Hat, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.ovirt.engine.sdk.generator.python;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ovirt.engine.sdk.entities.DetailedLink;
import org.ovirt.engine.sdk.generator.BrokerRules;
import org.ovirt.engine.sdk.generator.Location;
import org.ovirt.engine.sdk.generator.LocationRules;
import org.ovirt.engine.sdk.generator.SchemaRules;
import org.ovirt.engine.sdk.generator.python.templates.CollectionAddTemplate;
import org.ovirt.engine.sdk.generator.python.templates.CollectionGetNotSearchableTemplate;
import org.ovirt.engine.sdk.generator.python.templates.CollectionGetSearchableTemplate;
import org.ovirt.engine.sdk.generator.python.templates.CollectionListNotSearchableTemplate;
import org.ovirt.engine.sdk.generator.python.templates.CollectionListSearchableTemplate;
import org.ovirt.engine.sdk.generator.python.templates.CollectionTemplate;
import org.ovirt.engine.sdk.generator.python.utils.HeaderUtils;
import org.ovirt.engine.sdk.generator.python.utils.ParamUtils;
import org.ovirt.engine.sdk.generator.python.utils.ParamsContainer;
import org.ovirt.engine.sdk.generator.templates.AbstractTemplate;
import org.ovirt.engine.sdk.generator.utils.Tree;

public class Collection {
    public static String collection(String collectionName) {
        CollectionTemplate template = new CollectionTemplate();
        template.set("collection_name", collectionName);
        return template.evaluate();
    }

    public static String get(Tree<Location> collectionTree, DetailedLink link) {
        Tree<Location> entityLocation = collectionTree.getChild(LocationRules::isEntity);

        Object[] result = ParamUtils.getMethodParamsByUrlParamsMeta(link);
        String prmsStr = (String) result[0];
        Map<String, String> methodParams = (Map) result[1];
        Map<String, String> urlParams = (Map) result[2];

        result = HeaderUtils.generateMethodParams(link);
        String headersMethodParamsStr = (String) result[0];
        String headersMapParamsStr = (String) result[1];

        if (!headersMethodParamsStr.isEmpty()) {
            headersMethodParamsStr += ", ";
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("url", link.getHref());
        values.put("entity_broker_type", BrokerRules.getBrokerType(entityLocation));
        values.put("getter_name", SchemaRules.getElementName(collectionTree));
        values.put("headers_method_params_str", headersMethodParamsStr);
        values.put("headers_map_params_str", headersMapParamsStr);

        Map<String, String> docsParams = new LinkedHashMap<>();
        docsParams.put(ParamsContainer.ID_SEARCH_PARAM, "False");
        docsParams.put(ParamsContainer.NAME_SEARCH_PARAM, "False");
        String docs = Documentation.document(link, docsParams, new LinkedHashMap<>());
        values.put("docs", docs);

        // Capabilities resource has unique structure which is not
        // fully comply with RESTful collection pattern, but preserved
        // in sake of backward compatibility
        if (link.getHref().equals("capabilities")) {
            return CollectionExceptions.get(
                    link,
                headersMethodParamsStr,
                headersMapParamsStr,
                values
            );
        }

        // /disks search-by-name paradigm was broken by the engine
        // should be fixed later on
        if (link.getHref().equals("disks")) {
            return CollectionExceptions.get(
                    link,
                headersMethodParamsStr,
                headersMapParamsStr,
                values
            );
        }

        AbstractTemplate template;
        if (urlParams.containsKey("search:query")) {
            template = new CollectionGetSearchableTemplate();
        }
        else {
            template = new CollectionGetNotSearchableTemplate();
        }

        template.set(values);
        return template.evaluate();
    }

    public static String list(Tree<Location> collectionTree, DetailedLink link) {
        Tree<Location> entityTree = collectionTree.getChild(LocationRules::isEntity);

        Object[] result = ParamUtils.getMethodParamsByUrlParamsMeta(link);
        String prmsStr = (String) result[0];
        Map<String, String> methodParams = (Map<String, String>) result[1];
        Map<String, String> urlParams = (Map<String, String>) result[2];

        result = HeaderUtils.generateMethodParams(link);
        String headersMethodParamsStr = (String) result[0];
        String headersMapParamsStr = (String) result[1];

        Map<String, String> methodParamsCopy = new LinkedHashMap<>(methodParams);
        methodParams.put("**kwargs", "**kwargs");

        Map<String, String> values = new LinkedHashMap<>();
        values.put("url", link.getHref());
        values.put("entity_broker_type", BrokerRules.getBrokerType(entityTree));
        values.put("getter_name", SchemaRules.getElementName(collectionTree));

        // Capabilities resource has unique structure which is not
        // fully comply with RESTful collection pattern, but preserved
        // in sake of backward compatibility
        if (link.getHref().equals("capabilities")) {
            return CollectionExceptions.list();
        }
        else if (!prmsStr.isEmpty() || !headersMethodParamsStr.isEmpty()) {
            String combinedMethodParams = prmsStr + (
                !prmsStr.isEmpty() && !headersMethodParamsStr.isEmpty()? ", ": ""
                ) + headersMethodParamsStr;

            Map<String, String> docsParams = new LinkedHashMap<>();
            docsParams.put(ParamsContainer.KWARGS_PARAMS, "False");
            docsParams.put(ParamsContainer.QUERY_PARAMS, "False");
            String docs = Documentation.document(link, docsParams, methodParams);
            values.put("docs", docs);

            values.put("url_params",
                ParamUtils.toDictStr(
                    urlParams.keySet(),
                    methodParamsCopy.keySet()
                )
            );

            values.put("headers_map_params_str", headersMapParamsStr);
            values.put("combined_method_params", combinedMethodParams);

            CollectionListSearchableTemplate template = new CollectionListSearchableTemplate();
            template.set(values);
            return template.evaluate();
        }
        else {
            Map<String, String> docsParams = new LinkedHashMap<>();
            docsParams.put(ParamsContainer.KWARGS_PARAMS, "False");
            String docs = Documentation.document(link, docsParams, new LinkedHashMap<>());
            values.put("docs", docs);

            CollectionListNotSearchableTemplate template = new CollectionListNotSearchableTemplate();
            template.set(values);
            return template.evaluate();
        }
    }

    public static String add(Tree<Location> collectionTree, DetailedLink link) {
        Tree<Location> entityTree = collectionTree.getChild(LocationRules::isEntity);
        String elementName = SchemaRules.getElementName(collectionTree);

        Object[] result = ParamUtils.getMethodParamsByUrlParamsMeta(link);
        String prmsStr = (String) result[0];
        Map<String, String> methodParams = (Map<String, String>) result[1];
        Map<String, String> urlParams = (Map<String, String>) result[2];

        result = HeaderUtils.generateMethodParams(link);
        String headersMethodParamsStr = (String) result[0];
        String headersMapParamsStr = (String) result[1];

        StringBuilder combinedMethodParams = new StringBuilder();
        if (!headersMethodParamsStr.isEmpty()) {
            combinedMethodParams.append(", ");
            combinedMethodParams.append(headersMethodParamsStr);
        }
        if (!prmsStr.isEmpty()) {
            combinedMethodParams.append(", ");
            combinedMethodParams.append(prmsStr);
        }

        CollectionAddTemplate template = new CollectionAddTemplate();
        template.set("parameter_name", elementName.replaceAll("_", ""));
        template.set("url", link.getHref());
        template.set("entity_broker_type", BrokerRules.getBrokerType(entityTree));
        template.set("headers_map_params_str", headersMapParamsStr);
        template.set("url_query_params", ParamUtils.toDictStr(urlParams.keySet(), methodParams.keySet()));
        template.set("combined_method_params", combinedMethodParams);
        template.set("docs", Documentation.document(link, Collections.emptyMap(), methodParams));

        return template.evaluate();
    }
}