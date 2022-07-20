package com.redis.searchstock.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.redis.searchstock.service.RediSearchService;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FileTraverse extends SimpleFileVisitor<Path> {

        // Print information about
        // each type of file.
        List<File> fileList = new ArrayList<>();
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr) throws IOException {

            if (attr.isSymbolicLink()) {
                System.out.format("Symbolic link: %s ", file);
            } else if (attr.isRegularFile()) {
                System.out.format("Regular file: %s ", file);
                fileList.add(file.toFile());
                // rediSearchService.loadOneFile(file.toFile());
            } else {
                System.out.format("Other: %s ", file);
            }
            System.out.println("(" + attr.size() + "bytes)");
            return CONTINUE;
        }

        // Print each directory visited.
        @Override
        public FileVisitResult postVisitDirectory(Path dir,
                                                  IOException exc) {
            System.out.format("Directory: %s%n", dir);
            return CONTINUE;
        }

        // If there is some error accessing
        // the file, let the user know.
        // If you don't override this method
        // and an error occurs, an IOException
        // is thrown.
        @Override
        public FileVisitResult visitFileFailed(Path file,
                                               IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }

        public List<File> getFileList () {
            return fileList;
    }
}
