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

package de.kaiserpfalzedv.commons.core.files;

import de.kaiserpfalzedv.commons.core.resources.Metadata;
import de.kaiserpfalzedv.commons.core.resources.Pointer;
import de.kaiserpfalzedv.commons.core.store.OptimisticLockStoreException;
import de.kaiserpfalzedv.commons.test.AbstractTestBase;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestMemoryUserStore -- checks if the memory store behaves correctly.
 *
 * @author klenkes74 {@literal <rlichti@kaiserpfalz-edv.de>}
 * @since 2.0.0  2021-05-24
 */
@QuarkusTest
@Slf4j
public class TestMemoryFileStore extends AbstractTestBase {
    private static final UUID DATA_UID = UUID.randomUUID();
    private static final String DATA_NAMESPACE = "testNS";
    private static final String DATA_NAME = "testName";
    private static final OffsetDateTime DATA_CREATED = OffsetDateTime.now(Clock.systemUTC());
    private static final String DATA_API_KEY = "test-api-key";
    private static final String BASE64_DATA = "RGFzIGhpZXIgaXN0IGVpbmZhY2ggbnVyIGVpbiBCZWlzcGllbGZpbGUK";

    private static final UUID OTHER_UID = UUID.randomUUID();
    private static final String OTHER_NAMESPACE = "otherNS";
    private static final String OTHER_NAME = "otherName";
    private static final OffsetDateTime OTHER_CREATED = OffsetDateTime.now(Clock.systemUTC());

    private static final File DATA = File.builder()
            .metadata(
                    generateMetadata(DATA_NAMESPACE, DATA_NAME, DATA_UID, DATA_CREATED, null)
            )
            .spec(
                    FileData.builder()
                            .file(
                                    FileDescription.builder()
                                            .name(DATA_API_KEY)
                                            .mediaType(MediaType.APPLICATION_JSON)
                                            .data(BASE64_DATA.getBytes(StandardCharsets.UTF_8))
                                            .build()
                            )
                            .build()
            )
            .build();

    private static final File OTHER = File.builder()
            .metadata(
                    generateMetadata(OTHER_NAMESPACE, OTHER_NAME, OTHER_UID, OTHER_CREATED, null)
            )
            .spec(
                    FileData.builder()
                            .file(
                                    FileDescription.builder()
                                            .name(DATA_API_KEY)
                                            .mediaType(MediaType.APPLICATION_JSON)
                                            .data(BASE64_DATA.getBytes(StandardCharsets.UTF_8))
                                            .build()
                            )
                            .build()
            )
            .build();

    /**
     * service under test.
     */
    private final FileStoreService sut = new MemoryFileStore();


    @PostConstruct
    void init() {
        setTestSuite(getClass().getSimpleName());
        setLog(log);
    }


    @Test
    void shouldBeAMemoryUserStoreService() {
        startTest("store-is-memory-based");

        assertTrue(sut instanceof MemoryFileStore);
    }

    @Test
    void shouldSaveNewDataWhenDataIsNotStoredYet() {
        startTest("store-new-data");

        sut.save(DATA);

        Optional<File> result = sut.findByNameSpaceAndName(DATA_NAMESPACE, DATA_NAME);
        log.trace("result: {}", result);

        assertTrue(result.isPresent(), "The data should have been stored!");
        assertEquals(DATA, result.get());
    }

    @Test
    void shouldSaveNewDataWhenDataIsAlreadyStoredYet() {
        startTest("update-stored-data");

        sut.save(DATA); // store data first time

        sut.save(DATA); // update data

        Optional<File> result = sut.findByNameSpaceAndName(DATA_NAMESPACE, DATA_NAME);
        log.trace("result: {}", result);

        assertTrue(result.isPresent(), "The data should have been stored!");
        Assertions.assertNotEquals(DATA.getGeneration(), result.get().getGeneration());

        Assertions.assertEquals(1, result.get().getGeneration());
    }

    @Test
    public void shouldSaveOtherDataSetsWhenDataIsAlreadyStored() {
        startTest("save-other-data");

        sut.save(DATA);

        sut.save(OTHER);

        Optional<File> result = sut.findByUid(OTHER_UID);

        assertTrue(result.isPresent(), "there should be a user resource defined by this UID!");
        assertEquals(OTHER, result.get());
    }

    @Test
    public void shouldDeleteByNameWhenTheDataExists() {
        startTest("delete-existing-by-name");

        sut.save(DATA);
        sut.remove(DATA_NAMESPACE, DATA_NAME);

        Optional<File> result = sut.findByUid(DATA_UID);
        assertFalse(result.isPresent(), "Data should have been deleted!");
    }

    @Test
    public void shouldDeleteByUidWhenTheDataExists() {
        startTest("delete-existing-by-uid");

        sut.save(DATA);
        sut.remove(DATA_UID);

        Optional<File> result = sut.findByUid(DATA_UID);
        assertFalse(result.isPresent(), "Data should have been deleted!");
    }

    @Test
    public void shouldDeleteByObjectWhenTheDataExists() {
        startTest("delete-existing-by-uid");

        sut.save(DATA);
        sut.remove(DATA);

        Optional<File> result = sut.findByUid(DATA_UID);
        assertFalse(result.isPresent(), "Data should have been deleted!");
    }

    @Test
    public void shouldDeleteByNameWhenTheDataDoesNotExists() {
        startTest("delete-non-existing-by-name");

        sut.remove(DATA_NAMESPACE, DATA_NAME);

        Optional<File> result = sut.findByUid(DATA_UID);
        assertFalse(result.isPresent(), "Data should have been deleted!");
    }

    @Test
    public void shouldDeleteByUidWhenTheDataDoesNotExists() {
        startTest("delete-non-existing-by-uid");

        sut.remove(DATA_UID);

        Optional<File> result = sut.findByUid(DATA_UID);
        assertFalse(result.isPresent(), "Data should have been deleted!");
    }

    @Test
    public void shouldDeleteByObjectWhenTheDataDoesNotExists() {
        startTest("delete-non-existing-by-uid");

        sut.remove(DATA);

        Optional<File> result = sut.findByUid(DATA_UID);
        assertFalse(result.isPresent(), "Data should have been deleted!");
    }

    @Test
    void shouldThrowOptimisticLockExceptionWhenTheNewGenerationIsNotHighEnough() {
        startTest("throw-optimistic-lock-exception");

        sut.save(
                DATA.toBuilder()
                        .metadata(
                                Metadata.builder()
                                        .identity(
                                                Pointer.builder()
                                                        .kind(File.KIND)
                                                        .apiVersion(File.API_VERSION)
                                                        .nameSpace(DATA_NAMESPACE)
                                                        .name(DATA_NAME)
                                                        .build()
                                        )
                                        .generation(100)
                                        .build()
                        )
                        .build()
        );

        try {
            sut.save(DATA);

            fail("There should have been an OptimisticLockStoreException!");
        } catch (OptimisticLockStoreException e) {
            // every thing is fine. We wanted this exception
        }
    }


    /**
     * Sets up a metadata set.
     *
     * @return The generated metadata
     */
    private static Metadata generateMetadata(
            final String nameSpace,
            final String name,
            final UUID uid,
            final OffsetDateTime created,
            @SuppressWarnings("SameParameterValue") final OffsetDateTime deleted
    ) {
        return Metadata.builder()
                .identity(Pointer.builder()
                        .kind(File.KIND)
                        .apiVersion(File.API_VERSION)
                        .nameSpace(nameSpace)
                        .name(name)
                        .build()
                )
                .uid(uid)
                .created(created)
                .deleted(deleted)
                .build();
    }
}
