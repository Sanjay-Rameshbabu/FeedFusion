package com.feedfusion2.repository;

import com.feedfusion2.model.FeedPost;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedPostRepository extends MongoRepository<FeedPost, String> {

    // Find by unique link to check for existence before saving
    Optional<FeedPost> findByLink(String link);

    // Find by platform and keyword (case-insensitive regex on title or description)
    @Query("{ 'platform': ?0, $or: [ { 'title': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }")
    List<FeedPost> findByPlatformAndKeyword(String platform, String keyword, Sort sort);

    // Find by keyword only (case-insensitive regex on title or description)
    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<FeedPost> findByKeyword(String keyword, Sort sort);

    // Find by platform only
    List<FeedPost> findByPlatform(String platform, Sort sort);

    // Find all (used when no filter/search) - Spring Data provides findAll(Sort sort)
}