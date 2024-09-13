package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    public final static Long TEST_TODO_ID = 1L;
    public final static AuthUser TEST_AUTHUSER_ROLE_USER1 = new AuthUser(1L, "email1", UserRole.USER);
    public final static AuthUser TEST_AUTHUSER_ROLE_USER2 = new AuthUser(2L, "email2", UserRole.USER);
    public final static User TEST_USER1 = User.fromAuthUser(TEST_AUTHUSER_ROLE_USER1);
    public final static Todo TEST_TODO1 = new Todo("title", "contents", "weather", TEST_USER1);
    public final static CommentSaveRequest TEST_COMMENT_SAVE_REQUEST_DTO = new CommentSaveRequest("contents");

    private static final Logger log = LoggerFactory.getLogger(CommentServiceTest.class);
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private CommentService commentService;

    @Nested
    class SaveCommentTest {

        @Test
        public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
            // given
            long todoId = TEST_TODO_ID;
            CommentSaveRequest request = TEST_COMMENT_SAVE_REQUEST_DTO;
            AuthUser authUser = TEST_AUTHUSER_ROLE_USER1;

            given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                commentService.saveComment(authUser, todoId, request);
            });

            // then
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        public void comment를_정상적으로_등록한다() {
            // given
            long todoId = TEST_TODO_ID;
            CommentSaveRequest request = TEST_COMMENT_SAVE_REQUEST_DTO;
            AuthUser authUser = TEST_AUTHUSER_ROLE_USER1;
            User user = TEST_USER1;
            Todo todo = TEST_TODO1;     // Todo 객체가 생성될 때 자동으로 해당 user를 manager로 추가한다.
            Comment comment = new Comment(request.getContents(), user, todo);

            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(commentRepository.save(any())).willReturn(comment);

            // when
            CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

            // then
            assertNotNull(result);
        }

        @Test
        public void comment를_등록시_manager가_아니면_에러발생() {
            // given
            long todoId = TEST_TODO_ID;
            CommentSaveRequest request = TEST_COMMENT_SAVE_REQUEST_DTO;
            AuthUser manager = TEST_AUTHUSER_ROLE_USER2;
            Todo todo = TEST_TODO1;

            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                commentService.saveComment(manager, todoId, request);
            });

            // then
            assertEquals("해당 일정의 담당자가 아닙니다.", exception.getMessage());
        }
    }

    @Nested
    class GetCommentTest {

        @Test
        public void comment_목록_조회_성공(){
            // given
            long todoId = TEST_TODO_ID;
            User user = TEST_USER1;
            Todo todo = TEST_TODO1;
            ReflectionTestUtils.setField(todo, "id", todoId);

            CommentSaveRequest request = TEST_COMMENT_SAVE_REQUEST_DTO;
            Comment comment = new Comment(request.getContents(), user, todo);
            List<Comment> commentList = new ArrayList<>(Arrays.asList(comment));

            given(commentRepository.findByTodoIdWithUser(anyLong())).willReturn(commentList);

            // when
            List<CommentResponse> commentResponses = commentService.getComments(todoId);

            // then
            assertEquals(1, commentResponses.size());
            assertEquals(comment.getId(), commentResponses.get(0).getId());
            assertEquals(comment.getUser().getEmail(), commentResponses.get(0).getUser().getEmail());
        }
    }
}
