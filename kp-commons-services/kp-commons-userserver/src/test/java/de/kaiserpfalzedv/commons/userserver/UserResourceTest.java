/*
 * Copyright (c) 2022 Kaiserpfalz EDV-Service, Roland T. Lichti.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.kaiserpfalzedv.commons.userserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kaiserpfalzedv.commons.core.resources.History;
import de.kaiserpfalzedv.commons.core.resources.Metadata;
import de.kaiserpfalzedv.commons.core.resources.Pointer;
import de.kaiserpfalzedv.commons.core.resources.Status;
import de.kaiserpfalzedv.commons.core.user.User;
import de.kaiserpfalzedv.commons.core.user.UserData;
import de.kaiserpfalzedv.commons.test.AbstractTestBase;
import de.kaiserpfalzedv.commons.userserver.services.UserResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * UserResourceTest --
 *
 * @author klenkes74 {@literal <rlichti@kaiserpfalz-edv.de>}
 * @since 2.0.0  2022-01-01
 */
@QuarkusTest
@TestHTTPEndpoint(UserResource.class)
@Slf4j
public class UserResourceTest extends AbstractTestBase {
    private static final UUID UPDATEABLE_UUID = UUID.fromString("39062d79-e1a9-437a-b1b4-7dc783bd9eb2");
    private static final String UPDATEABLE_NAMESPACE = "name.namespace";
    private static final String UPDATEABLE_NAME = "name.name";

    private RestAssuredConfig restAssuredConfig;

    private UUID entityUid = UPDATEABLE_UUID;

    @Inject
    ObjectMapper mapper;

    @PostConstruct
    void init() {
        setTestSuite(getClass().getSimpleName());
        setLog(log);

        RestAssured.defaultParser = Parser.JSON;
        restAssuredConfig = RestAssuredConfig.config().objectMapperConfig(
                new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> mapper)
        );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    public void shouldReturnFullListWhenCalledWithoutParameters() {
        startTest("list-files");

        given()
                .when()
                .get()
                .then()
                .statusCode(200);
    }


    @Test
    @TestSecurity(user = "user", roles = "user")
    public void shouldReturnTheSelectedUserWhenGivenCorrectNameSpaceAndName() {
        startTest("retrieve-by-namespace-and-name");

        given()
                .when()
                .get("fileserver/liq-files-data")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    public void shouldNotFindTheUserWhenNameisInvalid() {
        startTest("failed-retrieve-by-unknown-name");

        given()
                .when()
                .get("fileserver/invalid-name")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    public void shouldNotFindTheUserWhenNameSpaceisInvalid() {
        startTest("failed-retrieve-by-unknown-namespace");

        given()
                .when()
                .get("invalid-namespace/liq-files-data")
                .then()
                .statusCode(404);
    }

    @Transactional
    @Test
    @TestSecurity(user = "user", roles = "user")
    public void shouldSaveTheUserWhenNotExisting() {
        startTest("save-owned-file");

        User request = User.builder()
                .metadata(
                        Metadata.builder()
                                .identity(
                                        Pointer.builder()
                                                .kind(User.KIND)
                                                .apiVersion(User.API_VERSION)
                                                .nameSpace("single-file")
                                                .name("single-file-name")
                                                .build()
                                )
                                .labels(Map.of(
                                        UserData.ISSUER, "issuer",
                                        UserData.SUBJECT, "subject"
                                ))
                                .build()
                )
                .spec(
                        UserData.builder()
                                .name("single-file-name")
                                .description("user")
                                .picture(
                                        Pointer.builder()
                                                .kind(User.KIND)
                                                .apiVersion(User.API_VERSION)
                                                .nameSpace("single-file")
                                                .name("single-file-name")
                                                .build()
                                )
                                .build()
                )
                .status(
                        Status.builder()
                                .observedGeneration(0)
                                .history(List.of(
                                        History.builder()
                                                .status("created")
                                                .timeStamp(OffsetDateTime.now(ZoneOffset.UTC))
                                                .message("User test-file.txt with preview created.")
                                                .build()
                                ))
                                .build()
                )
                .build();

        User result = given()
                .config(restAssuredConfig)
                .when()
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .prettyPeek()
                .then()
                .statusCode(200)
                .extract().response()
                .as(User.class);
//                .contentType(ContentType.JSON).extract().jsonPath().peek().get();

        log.info("Result. data={}", result);
        entityUid = result.getUid();

        log.info("Result. id={}, data={}", entityUid, result);
    }

    @Transactional
    @Test
    @TestSecurity(user = "user", roles = "user")
    public void shouldUpdateTheUserWhenUserMatch() {
        startTest("update-owned-file");

        User request = User.builder()
                .metadata(
                        Metadata.builder()
                                .identity(
                                        Pointer.builder()
                                                .kind(User.KIND)
                                                .apiVersion(User.API_VERSION)
                                                .nameSpace(UPDATEABLE_NAMESPACE)
                                                .name(UPDATEABLE_NAME)
                                                .build()
                                )
                                .uid(entityUid)
                                .created(OffsetDateTime.now(ZoneOffset.UTC))
                                .labels(Map.of(
                                        UserData.ISSUER, "issuer",
                                        UserData.SUBJECT, "subject"
                                ))
                                .build()
                )
                .spec(
                        UserData.builder()
                                .name(UPDATEABLE_NAME)
                                .build()
                )
                .status(
                        Status.builder()
                                .observedGeneration(0)
                                .history(List.of(
                                        History.builder()
                                                .status("created")
                                                .timeStamp(OffsetDateTime.now(ZoneOffset.UTC))
                                                .message("User test-file.txt with preview created.")
                                                .build()
                                ))
                                .build()
                )
                .build();

        User result = given()
                .config(restAssuredConfig)
                .when()
                .contentType(ContentType.JSON)
                .body(request)
                .put()
                .prettyPeek()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response()
                .as(User.class);

        assertThat(result.getMetadata().getModified(), not(equals(request.getMetadata().getCreated())));
    }

    @Test
    @Order(3)
    @TestSecurity(user = "user", roles = "user")
    public void shouldDeleteTheUserWhenNameSpaceAndNameIsGiven() {
        startTest("delete-by-namespace-and-name", UPDATEABLE_NAMESPACE, UPDATEABLE_NAME);
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("nameSpace", UPDATEABLE_NAMESPACE)
                .pathParam("name", UPDATEABLE_NAME)
                .delete("/{nameSpace}/{name}")
                .prettyPeek()
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity
    public void shouldReturnNotAllowedWhenNotAuthenticated() {
        startTest("fail-unauthenticated");

        given()
                .when()
                .get()
                .then()
                .statusCode(401);
    }
}
