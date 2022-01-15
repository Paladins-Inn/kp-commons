/*
 * Copyright (c) 2022 Kaiserpfalz EDV-Service, Roland T. Lichti
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.kaiserpfalzedv.commons.core.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;

/**
 * ResourcePointer -- Identifies a single resource.
 *
 * @author klenkes74 {@literal <rlichti@kaiserpfalz-edv.de>}
 * @version 2.0.2  2021-01-04
 * @since 2.0.0  2021-05-24
 */
public interface ResourcePointer extends Serializable, Cloneable {
    String getKind();

    String getApiVersion();

    String getNameSpace();

    String getName();

    @Schema(
            name = "selfLink",
            description = "The local part of the URL to retrieve the resource.",
            nullable = true,
            readOnly = true,
            example = "/api/v1/Resource/default/name",
            minLength = 8,
            maxLength = 100
    )
    default String getSelfLink() {
        return String.format("/api/%s/%s/%s", getApiVersion(), getKind(), getNameSpace(), getName());
    }

}
