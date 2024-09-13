package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    private final static AuthUser TEST_AUTHUSER1 = new AuthUser(1L, "auth1@naver.com", UserRole.USER);
    private final static User TEST_USER1 = new User("Password1" , "auth1@naver.com", UserRole.USER);
    private final static User TEST_USER2 = new User("Password1" , "auth1@naver.com", UserRole.USER);

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Nested
    class SaveManagerTest {

        @Test
        void todo의_user가_null인_경우_예외가_발생한다() {
            // given
            AuthUser authUser = TEST_AUTHUSER1;
            long todoId = 1L;
            long managerUserId = 2L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", null);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("일정을 만든 유저가 유효하지 않습니다.", exception.getMessage()); // 어색한 문장 수정(기능변경 X)
        }

        @Test
        void 현재_유저와_todo_유저가_다를경우_예외가_발생한다() {
            // given
            AuthUser authUser = TEST_AUTHUSER1;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", 2L);
            long todoId = 1L;
            long managerUserId = 3L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", user);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("현재 유저와 일정을 만든 유저가 일치하지 않습니다.", exception.getMessage());
        }

        @Test
        void 등록하려는_담당자가_없는_경우_예외가_발생한다() {
            // given
            AuthUser authUser = TEST_AUTHUSER1;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", 1L);
            long todoId = 1L;
            long managerUserId = 2L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", user);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("등록하려고 하는 담당자 유저가 존재하지 않습니다.", exception.getMessage());
        }

        @Test
        void 본인을_담당자로_지정할_경우_예외가_발생한다() {
            // given
            AuthUser authUser = TEST_AUTHUSER1;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", 1L);
            long todoId = 1L;
            long managerUserId = 1L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", user);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", exception.getMessage());
        }

        @Test // 테스트코드 샘플
        void manager가_정상적으로_등록된다() {
            // given
            AuthUser authUser = TEST_AUTHUSER1;
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            long todoId = 1L;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerUserId = 2L;
            User managerUser = TEST_USER2;  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
            given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

            // then
            assertNotNull(response);
            assertEquals(managerUser.getId(), response.getUser().getId());
            assertEquals(managerUser.getEmail(), response.getUser().getEmail());
        }
    }

    @Nested
    class GetManagerTest{

        @Test
        public void manager_목록_조회_시_Todo가_없다면_IRE_에러를_던진다() {
            // given
            long todoId = 1L;
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test // 테스트코드 샘플
        public void manager_목록_조회에_성공한다() {
            // given
            long todoId = 1L;
            User user = TEST_USER1;
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager mockManager = new Manager(todo.getUser(), todo);
            List<Manager> managerList = List.of(mockManager);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

            // when
            List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

            // then
            assertEquals(1, managerResponses.size());
            assertEquals(mockManager.getId(), managerResponses.get(0).getId());
            assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
        }
    }


    @Nested
    class DeleteManagerTest {

        @Test
        public void 해당_User가_없다면_예외_발생(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> managerService.deleteManager(userId, todoId, managerId));

            // then
            assertEquals("User not found", exception.getMessage());
        }

        @Test
        public void 해당_Todo가_없다면_예외_발생(){
            // given
            long userId = 1L;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", userId);

            long todoId = 1L;
            long managerId = 1L;

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> managerService.deleteManager(userId, todoId, managerId));

            // then
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        public void Todo의_작성자가_아닐경우_예외_발생(){
            // given
            long userId = 1L;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", userId);
            User user2 = TEST_USER2;
            ReflectionTestUtils.setField(user, "id", 2L);

            long todoId = 1L;
            Todo todo = new Todo("Title", "Contents", "Sunny", user2);
            ReflectionTestUtils.setField(todo, "id", todoId);

            long managerId = 1L;

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> managerService.deleteManager(userId, todoId, managerId));

            // then
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        public void 삭제할_매니저가_없다면_예외_발생(){
            // given
            long userId = 1L;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", userId);

            long todoId = 1L;
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            long managerId = 1L;

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(managerRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> managerService.deleteManager(userId, todoId, managerId));

            // then
            assertEquals("Manager not found", exception.getMessage());
        }

        @Test
        public void Todo의_담당자가_아닐경우_예외_발생(){
            // given
            long userId = 1L;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", userId);
            User user2 = TEST_USER2;
            ReflectionTestUtils.setField(user, "id", 2L);

            long todoId = 1L;
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);
            Todo todo2 = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", 2L);

            long managerId = 1L;
            Manager manager = new Manager(user2, todo2);
            ReflectionTestUtils.setField(manager, "id", managerId);

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(managerRepository.findById(anyLong())).willReturn(Optional.of(manager));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> managerService.deleteManager(userId, todoId, managerId));

            // then
            assertEquals("해당 일정에 등록된 담당자가 아닙니다.", exception.getMessage());
        }

        @Test
        public void 매니저_삭제_성공(){
            // given
            long userId = 1L;
            User user = TEST_USER1;
            ReflectionTestUtils.setField(user, "id", userId);
            User user2 = TEST_USER2;
            ReflectionTestUtils.setField(user, "id", 2L);

            long todoId = 1L;
            Todo todo = new Todo("Title", "Contents", "Sunny", user);

            long managerId = 1L;
            Manager manager = new Manager(user2, todo);

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(managerRepository.findById(anyLong())).willReturn(Optional.of(manager));

            // when
            managerService.deleteManager(userId, todoId, managerId);

            // then
            verify(managerRepository).delete(manager);
        }

    }

}
