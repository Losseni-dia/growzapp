CREATE TABLE `user_langues` (
    `user_id` BIGINT NOT NULL,
    `langue_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `langue_id`),
    CONSTRAINT `fk_user_langues_user` 
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_langues_langue` 
        FOREIGN KEY (`langue_id`) REFERENCES `langues`(`id`) ON DELETE CASCADE
);
