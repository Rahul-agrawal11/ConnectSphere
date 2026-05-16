package com.connectsphere.post.service;

import com.connectsphere.post.dto.request.CreatePostRequest;
import com.connectsphere.post.dto.request.UpdatePostRequest;
import com.connectsphere.post.dto.response.PostResponse;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.enums.PostType;
import com.connectsphere.post.enums.PostVisibility;
import com.connectsphere.post.exception.PostNotFoundException;
import com.connectsphere.post.exception.UnauthorizedActionException;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private Post post;

    @BeforeEach
    void setUp() {
        post = Post.builder()
                .id(1L)
                .authorId(10L)
                .content("Hello ConnectSphere")
                .postType(PostType.TEXT)
                .visibility(PostVisibility.PUBLIC)
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPost_ShouldCreatePostSuccessfully() {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello ConnectSphere");
        request.setVisibility(PostVisibility.PUBLIC);
        request.setMediaUrls(List.of("image1.jpg"));

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PostResponse response = postService.createPost(10L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getAuthorId());
        assertEquals("Hello ConnectSphere", response.getContent());
        assertEquals("PUBLIC", response.getVisibility());
        assertEquals("MEDIA", response.getPostType());

        verify(postRepository).save(any(Post.class));
    }

    @Test
    void getPostById_ShouldReturnPost_WhenPostIsPublic() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        PostResponse response = postService.getPostById(1L, 99L);

        assertNotNull(response);
        assertEquals("Hello ConnectSphere", response.getContent());
    }

    @Test
    void getPostById_ShouldThrowException_WhenPostNotFound() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.getPostById(1L, 10L));
    }

    @Test
    void getPostById_ShouldThrowException_WhenPrivatePostAndRequesterIsNotOwner() {
        post.setVisibility(PostVisibility.PRIVATE);

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedActionException.class, () -> postService.getPostById(1L, 99L));
    }

    @Test
    void updatePost_ShouldUpdatePost_WhenRequesterIsOwner() {
        UpdatePostRequest request = new UpdatePostRequest();
        request.setContent("Updated content");
        request.setVisibility(PostVisibility.PRIVATE);
        request.setMediaUrls(List.of("updated-image.jpg"));

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.updatePost(1L, 10L, request);

        assertEquals("Updated content", response.getContent());
        assertEquals("PRIVATE", response.getVisibility());
        assertEquals("MEDIA", response.getPostType());

        verify(postRepository).save(post);
    }

    @Test
    void updatePost_ShouldThrowException_WhenRequesterIsNotOwner() {
        UpdatePostRequest request = new UpdatePostRequest();
        request.setContent("Updated content");

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedActionException.class, () -> postService.updatePost(1L, 99L, request));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_ShouldSoftDeletePost_WhenRequesterIsOwner() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        postService.deletePost(1L, 10L);

        assertTrue(post.getIsDeleted());
        verify(postRepository).save(post);
    }

    @Test
    void deletePost_ShouldThrowException_WhenRequesterIsNotOwner() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedActionException.class, () -> postService.deletePost(1L, 99L));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void getPostsByUser_ShouldReturnAllPosts_WhenRequesterIsAuthor() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post));

        when(postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(10L, pageable))
                .thenReturn(page);

        Page<PostResponse> response = postService.getPostsByUser(10L, 10L, pageable);

        assertEquals(1, response.getContent().size());
        verify(postRepository).findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(10L, pageable);
    }

    @Test
    void getPostsByUser_ShouldReturnOnlyPublicPosts_WhenRequesterIsOtherUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post));

        when(postRepository.findByAuthorIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                10L, PostVisibility.PUBLIC, pageable))
                .thenReturn(page);

        Page<PostResponse> response = postService.getPostsByUser(10L, 99L, pageable);

        assertEquals(1, response.getContent().size());
        verify(postRepository).findByAuthorIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                10L, PostVisibility.PUBLIC, pageable);
    }

    @Test
    void getPublicFeed_ShouldReturnPublicPosts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post));

        when(postRepository.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                PostVisibility.PUBLIC, pageable))
                .thenReturn(page);

        Page<PostResponse> response = postService.getPublicFeed(pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void getFeedForUser_ShouldReturnPublicFeed_WhenFollowedListIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post));

        when(postRepository.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                PostVisibility.PUBLIC, pageable))
                .thenReturn(page);

        Page<PostResponse> response = postService.getFeedForUser(List.of(), pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void searchPosts_ShouldReturnMatchingPosts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post));

        when(postRepository.searchByContent("hello", pageable)).thenReturn(page);

        Page<PostResponse> response = postService.searchPosts("hello", pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void changeVisibility_ShouldUpdateVisibility_WhenOwner() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.changeVisibility(1L, 10L, PostVisibility.PRIVATE);

        assertEquals("PRIVATE", response.getVisibility());
    }

    @Test
    void incrementLikes_ShouldWork_WhenPostExists() {
        when(postRepository.incrementLikesCount(1L)).thenReturn(1);

        postService.incrementLikes(1L);

        verify(postRepository).incrementLikesCount(1L);
    }

    @Test
    void incrementLikes_ShouldThrowException_WhenPostNotFound() {
        when(postRepository.incrementLikesCount(1L)).thenReturn(0);

        assertThrows(PostNotFoundException.class, () -> postService.incrementLikes(1L));
    }

    @Test
    void incrementComments_ShouldWork_WhenPostExists() {
        when(postRepository.incrementCommentsCount(1L)).thenReturn(1);

        postService.incrementComments(1L);

        verify(postRepository).incrementCommentsCount(1L);
    }

    @Test
    void getPostCount_ShouldReturnCount() {
        when(postRepository.countByAuthorIdAndIsDeletedFalse(10L)).thenReturn(5L);

        long count = postService.getPostCount(10L);

        assertEquals(5L, count);
    }

    @Test
    void adminDeletePost_ShouldSoftDeleteAnyPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.adminDeletePost(1L);

        assertTrue(post.getIsDeleted());
        verify(postRepository).save(post);
    }
}