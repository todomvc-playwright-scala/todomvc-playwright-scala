package todomvc_playwright_scala

import com.microsoft.playwright.*
import com.microsoft.playwright.Page.GetByRoleOptions
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import upickle.default.*

import scala.compiletime.uninitialized

class TodomvcSuite extends AnyFunSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers {
  var playwright: Playwright = uninitialized
  var browser: Browser = uninitialized
  var context: BrowserContext = uninitialized
  var page: Page = uninitialized

  private val TODO_ITEMS =
    Array("buy some cheese", "feed the cat", "book a doctors appointment")

  def withAllTodos(testCode: () => Any): Unit = {
    createDefaultTodos()
    checkNumberOfTodosInLocalStorage(3)
    testCode()
  }

  describe("New Todo") {
    it("should allow me to add todo items") {
      val newTodo = page.getByPlaceholder("What needs to be done?")
      newTodo.fill(TODO_ITEMS(0))
      newTodo.press("Enter")
      assertThat(page.locator(".view")).hasText(TODO_ITEMS(0))

      newTodo.fill(TODO_ITEMS(1))
      newTodo.press("Enter")
      assertThat(page.locator(".view"))
        .hasText(Array(TODO_ITEMS(0), TODO_ITEMS(1)))

      checkNumberOfTodosInLocalStorage(2)
    }
    it("should clear text input field when an item is added") {
      val newTodo = page.getByPlaceholder("What needs to be done?")
      newTodo.fill(TODO_ITEMS(0))
      newTodo.press("Enter")

      assertThat(newTodo).isEmpty()
      checkNumberOfTodosInLocalStorage(1)
    }
    it("should append new items to the bottom of the list") {
      createDefaultTodos()

      assertThat(page.getByText("3 items left")).isVisible()
      assertThat(page.locator(".todo-count")).hasText("3 items left")

      checkNumberOfTodosInLocalStorage(3)
    }
    it("should show #main and #footer when items added") {
      val newTodo = page.getByPlaceholder("What needs to be done?")
      newTodo.fill(TODO_ITEMS(0))
      newTodo.press("Enter")

      assertThat(page.locator(".main")).isVisible()
      assertThat(page.locator(".footer")).isVisible()

      checkNumberOfTodosInLocalStorage(1)
    }
  }
  describe("Mark all as completed") {
    def withAllTodos(testCode: () => Any): Unit = {
      createDefaultTodos()
      checkNumberOfTodosInLocalStorage(3)
      checkNumberOfCompletedTodosInLocalStorage(0)
      testCode()
      checkNumberOfTodosInLocalStorage(3)
    }
    it("should allow me to mark all items as completed") {
      withAllTodos(() => {
        page.getByLabel("Mark all as complete").click()
        assertThat(page.locator("ul.todo-list>li")).hasClass(Array("completed", "completed", "completed"))
        checkNumberOfCompletedTodosInLocalStorage(3)
      })
    }
    it("should allow me to clear the complete state of all items") {
      withAllTodos(() => {
        val toggleAll = page.getByLabel("Mark all as complete")
        toggleAll.check()
        toggleAll.uncheck()

        assertThat(page.locator("ul.todo-list>li")).hasClass(Array("", "", ""))
      })
    }
    it("complete all checkbox should update state when items are completed / cleared") {
      withAllTodos(() => {
        val toggleAll = page.getByLabel("Mark all as complete")
        toggleAll.check()
        assertThat(toggleAll).isChecked()
        checkNumberOfCompletedTodosInLocalStorage(3)

        val firstTodo = page.locator("ul.todo-list>li").nth(0)
        firstTodo.getByRole(AriaRole.CHECKBOX).uncheck()

        assertThat(toggleAll).not.isChecked()

        firstTodo.getByRole(AriaRole.CHECKBOX).check()
        checkNumberOfCompletedTodosInLocalStorage(3)

        assertThat(toggleAll).isChecked()
      })
    }
  }
  describe("Item") {
    it("should allow me to mark items as complete") {
      createDefaultTodos(2)
      val firstTodo = page.locator("ul.todo-list>li").nth(0)
      firstTodo.getByRole(AriaRole.CHECKBOX).check()
      assertThat(firstTodo).hasClass("completed")

      val secondTodo = page.locator("ul.todo-list>li").nth(1)
      assertThat(secondTodo).not.hasClass("completed")
      secondTodo.getByRole(AriaRole.CHECKBOX).check()

      assertThat(firstTodo).hasClass("completed")
      assertThat(secondTodo).hasClass("completed")
    }
    it("should allow me to un-mark items as complete") {
      createDefaultTodos(2)

      val firstTodo = page.locator("ul.todo-list>li").nth(0)
      val secondTodo = page.locator("ul.todo-list>li").nth(1)
      firstTodo.getByRole(AriaRole.CHECKBOX).check()
      assertThat(firstTodo).hasClass("completed")
      assertThat(secondTodo).not.hasClass("completed")
      checkNumberOfCompletedTodosInLocalStorage(1)

      firstTodo.getByRole(AriaRole.CHECKBOX).uncheck()
      assertThat(firstTodo).not.hasClass("completed")
      assertThat(secondTodo).not.hasClass("completed")
      checkNumberOfCompletedTodosInLocalStorage(0)
    }
    it("should allow me to edit an item") {
      createDefaultTodos()
      val todoItems = page.locator("ul.todo-list>li")
      val secondTodo = todoItems.nth(1)
      secondTodo.dblclick()
      assertThat(secondTodo.locator(".edit")).hasValue(TODO_ITEMS(1))
      secondTodo.locator(".edit").fill("buy some sausages")
      secondTodo.locator(".edit").press("Enter")

      assertThat(todoItems).hasText(Array(TODO_ITEMS(0), "buy some sausages", TODO_ITEMS(2)))
      checkTodosInLocalStorage("buy some sausages")
    }
  }
  describe("Editing") {
    it("should hide other controls when editing") {
      withAllTodos(() => {
        val todoItem = page.locator("ul.todo-list>li").nth(1)
        todoItem.dblclick()
        assertThat(todoItem.getByRole(AriaRole.CHECKBOX)).not.isVisible()
        assertThat(todoItem.locator("label", new Locator.LocatorOptions().setHasText(TODO_ITEMS(1)))).not.isVisible()

        checkNumberOfTodosInLocalStorage(3)
      })
    }
    it("should save edits on blur") {
      withAllTodos(() => {
        val todoItems = page.locator("ul.todo-list>li")
        todoItems.nth(1).dblclick()
        todoItems.nth(1).locator(".edit").fill("buy some sausages")
        todoItems.nth(1).locator(".edit").dispatchEvent("blur")

        assertThat(todoItems).hasText(Array(TODO_ITEMS(0), "buy some sausages", TODO_ITEMS(2)))
        checkTodosInLocalStorage("buy some sausages")
      })
    }
    it("should trim entered text") {
      withAllTodos(() => {
        val todoItems = page.locator("ul.todo-list>li")
        todoItems.nth(1).dblclick()
        todoItems.nth(1).locator(".edit").fill("   buy some sausages   ")
        todoItems.nth(1).locator(".edit").press("Enter")

        assertThat(todoItems).hasText(Array(TODO_ITEMS(0), "buy some sausages", TODO_ITEMS(2)))
        checkTodosInLocalStorage("buy some sausages")
      })
    }
    it("should remove the item if an empty text string was entered") {
      withAllTodos(() => {
        val todoItems = page.locator("ul.todo-list>li")
        todoItems.nth(1).dblclick()
        todoItems.nth(1).locator(".edit").fill("")
        todoItems.nth(1).locator(".edit").press("Enter")

        assertThat(todoItems).hasText(Array(TODO_ITEMS(0), TODO_ITEMS(2)))
      })
    }
    it("should cancel edits on escape") {
      withAllTodos(() => {
        val todoItems = page.locator("ul.todo-list>li")
        todoItems.nth(1).dblclick()
        todoItems.nth(1).locator(".edit").fill("   buy some sausages   ")
        todoItems.nth(1).locator(".edit").press("Escape")

        assertThat(todoItems).hasText(TODO_ITEMS)
      })
    }
  }
  describe("Counter") {
    it("should display the current number of todo items") {
      val newTodo = page.getByPlaceholder("What needs to be done?")

      newTodo.fill(TODO_ITEMS(0))
      newTodo.press("Enter")
      assertThat(page.locator(".todo-count").locator("strong")).containsText("1")

      newTodo.fill(TODO_ITEMS(1))
      newTodo.press("Enter")
      assertThat(page.locator(".todo-count").locator("strong")).containsText("2")

      checkNumberOfTodosInLocalStorage(2)
    }
  }
  describe("Clear completed button") {
    it("should display the correct text") {
      withAllTodos(() => {
        page.locator(".todo-list li .toggle").first().check()
        assertThat(page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Clear completed"))).isVisible()
      })
    }
    it("should remove completed items when clicked") {
      withAllTodos(() => {
        val todoItems = page.locator("ul.todo-list>li")
        todoItems.nth(1).getByRole(AriaRole.CHECKBOX).check()
        page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Clear completed")).click()
        assertThat(todoItems).hasCount(2)
        assertThat(todoItems).hasText(Array(TODO_ITEMS(0), TODO_ITEMS(2)))
      })
    }
    it("should be hidden when there are no items that are completed") {
      withAllTodos(() => {
        page.locator(".todo-list li .toggle").first().check();
        page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Clear completed")).click()
        assertThat(page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Clear completed"))).isHidden()
      })
    }
  }
  describe("Persistence") {
    it("should persist its data") {
      val newTodo = page.getByPlaceholder("What needs to be done?")

      createDefaultTodos(2)

      val todoItems = page.locator("ul.todo-list>li")
      todoItems.nth(0).getByRole(AriaRole.CHECKBOX).check()
      assertThat(todoItems).hasText(Array(TODO_ITEMS(0), TODO_ITEMS(1)))
      assertThat(page.locator("ul.todo-list>li")).hasClass(Array("completed", ""))

      checkNumberOfCompletedTodosInLocalStorage(1)

      page.reload()
      assertThat(todoItems).hasText(Array(TODO_ITEMS(0), TODO_ITEMS(1)))
      assertThat(page.locator("ul.todo-list>li")).hasClass(Array("completed", ""))
    }
  }
  describe("Routing") {
    it("should allow me to display active items") {
      withAllTodos(() => {
        page.locator(".todo-list li .toggle").nth(1).check()
        checkNumberOfCompletedTodosInLocalStorage(1)
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Active")).click()
        assertThat(page.locator(".view")).hasCount(2)
        assertThat(page.locator(".view")).hasText(Array(TODO_ITEMS(0), TODO_ITEMS(2)))
      })
    }
    it("should respect the back button") {
      withAllTodos(() => {
        page.locator(".todo-list li .toggle").nth(1).check()
        checkNumberOfCompletedTodosInLocalStorage(1)

        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("All")).click()
        assertThat(page.locator(".view")).hasCount(3)
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Active")).click()
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Completed")).click()
        assertThat(page.locator(".view")).hasCount(1)
        page.goBack()
        assertThat(page.locator(".view")).hasCount(2)
        page.goBack()
        assertThat(page.locator(".view")).hasCount(3)
      })
    }
    it("should allow me to display completed items") {
      withAllTodos(() => {
        page.locator(".todo-list li .toggle").nth(1).check()
        checkNumberOfCompletedTodosInLocalStorage(1)
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Completed")).click()
        assertThat(page.locator(".view")).hasCount(1)
      })
    }
    it("should allow me to display all items") {
      withAllTodos(() => {
        page.locator(".todo-list li .toggle").nth(1).check()
        checkNumberOfCompletedTodosInLocalStorage(1)
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Active")).click()
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Completed")).click()
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("All")).click()
        assertThat(page.locator(".view")).hasCount(3)

      })
    }
    it("should highlight the currently applied filter") {
      withAllTodos(() => {
        assertThat(page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("All"))).hasClass("selected")
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Active")).click()
        assertThat(page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Active"))).hasClass("selected")
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Completed")).click()
        assertThat(page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Completed"))).hasClass("selected")
      })
    }
  }

  private def checkNumberOfTodosInLocalStorage(
      expected: Int,
      filter: Todo => Boolean = _ => true
  ): Unit = getTodosFromLocalstorage.count(filter) shouldBe expected

  private def checkTodosInLocalStorage(title: String): Unit =
    getTodosFromLocalstorage.find(_.title == title) should not be empty

  private def getTodosFromLocalstorage: Seq[Todo] = {
    val json = ujson.read(context.storageState())
    val todosString = json("origins")(0)("localStorage")(0)("value").strOpt.get
    read[List[Todo]](todosString)
  }

  private def checkNumberOfCompletedTodosInLocalStorage(expected: Int): Unit =
    checkNumberOfTodosInLocalStorage(expected, _.completed)

  def createDefaultTodos(number: Int = TODO_ITEMS.size): Unit = {
    val newTodo = page.getByPlaceholder("What needs to be done?")
    TODO_ITEMS
      .take(number)
      .foreach(todo => {
        newTodo.fill(todo)
        newTodo.press("Enter")
      })
  }

  override protected def beforeAll(): Unit = {
    playwright = Playwright.create()
    browser = playwright.chromium().launch()
  }
  override protected def afterAll(): Unit = {
    browser.close()
    playwright.close()
  }

  override protected def beforeEach(): Unit = {
    context = browser.newContext
    page = context.newPage
    page.navigate("http://localhost:3000/")
  }
}
