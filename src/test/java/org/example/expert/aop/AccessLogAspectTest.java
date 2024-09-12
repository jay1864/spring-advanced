package org.example.expert.aop;

import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.service.CommentAdminService;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.domain.user.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class AccessLogAspectTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    @InjectMocks
    private CommentAdminService commentAdminService;

    @Test
    public void 사용자_역할_변경시_로그_출력(CapturedOutput output) throws Exception {
        // given
        User user = new User("test@naver.com", "Password1234", UserRole.ADMIN);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserRoleChangeRequest request = new UserRoleChangeRequest("user");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

        // when
        userAdminService.changeUserRole(1L, request);

        // then
        assertThat(output.getOut().contains("::: User ID : {}, Access Time : {}, URL : {} :::"));
    }

    @Test
    public void 댓글_삭제시_로그_출력(CapturedOutput output) throws Exception {
        //given
        User user = new User("test@naver.com", "Password1234", UserRole.ADMIN);
        ReflectionTestUtils.setField(user, "id", 1L);

        Todo todo = new Todo("Title:test", "Contents:test", "Weather", user);
        Comment comment = new Comment("Comment:test", user, todo);
        ReflectionTestUtils.setField(comment, "id", 1L);

        //when
        commentAdminService.deleteComment(1L);

        //then
        assertThat(output.getOut().contains("::: Comment ID : {}, Title : {}, Content : {}"));
    }
}