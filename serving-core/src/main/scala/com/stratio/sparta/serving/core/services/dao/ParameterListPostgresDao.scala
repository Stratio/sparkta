/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.serving.core.services.dao

import java.util.UUID

import com.stratio.sparta.core.properties.ValidatingPropertyMap._
import com.stratio.sparta.serving.core.constants.AppConstant
import com.stratio.sparta.serving.core.dao.ParameterListDao
import com.stratio.sparta.serving.core.exception.ServerException
import com.stratio.sparta.serving.core.models.parameters.{ParameterList, ParameterListAndContexts, ParameterListFromWorkflow, ParameterVariable}
import com.stratio.sparta.serving.core.utils.JdbcSlickConnection
import org.joda.time.DateTime
import slick.jdbc.PostgresProfile
import AppConstant._
import com.stratio.sparta.serving.core.models.workflow.Workflow
import org.json4s.jackson.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ParameterListPostgresDao extends ParameterListDao {

  override val profile = PostgresProfile
  override val db = JdbcSlickConnection.db

  import profile.api._

  lazy val workflowService = new WorkflowPostgresDao

  override def initializeData(): Unit = {
    val customDefaultsFuture = findByName(CustomExampleParameterList)
    val environmentFuture = findByName(EnvironmentParameterListName)

    customDefaultsFuture.onFailure { case _ =>
      log.debug("Initializing custom defaults list")
      for {
        _ <- createFromParameterList(ParameterList(
          name = CustomExampleParameterList,
          parameters = DefaultCustomExampleParameters
        ))
      } yield {
        log.debug("The custom defaults list initialization has been completed")
      }
    }
    customDefaultsFuture.onSuccess { case paramList =>
      val variablesNames = paramList.parameters.map(_.name)
      val variablesToAdd = DefaultCustomExampleParameters.filter { variable =>
        !variablesNames.contains(variable.name)
      }
      log.debug(s"Variables not present in the custom defaults list: $variablesToAdd")
      update(paramList.copy(parameters = variablesToAdd ++ paramList.parameters))
    }
    environmentFuture.onFailure { case _ =>
      log.debug("Initializing environment list")
      for {
        _ <- createFromParameterList(ParameterList(
          id = EnvironmentParameterListId,
          name = EnvironmentParameterListName,
          parameters = DefaultEnvironmentParameters
        ))
      } yield {
        log.debug("The environment list initialization has been completed")
      }
    }
    environmentFuture.onSuccess { case envList =>
      val variablesNames = envList.parameters.map(_.name)
      val variablesToAdd = DefaultEnvironmentParameters.filter { variable =>
        !variablesNames.contains(variable.name)
      }
      log.debug(s"Variables not present in the environment list: $variablesToAdd")
      update(envList.copy(parameters = variablesToAdd ++ envList.parameters))
    }

  }

  def findByParentWithContexts(parent: String): Future[ParameterListAndContexts] = {
    for {
      paramList <- findByName(parent)
      contexts <- findByParent(parent)
    } yield {
      ParameterListAndContexts(
        parameterList = paramList,
        contexts = contexts
      )
    }
  }

  def findByParent(parent: String): Future[Seq[ParameterList]] =
    db.run(table.filter(_.parent === Option(parent)).result)

  def findByName(name: String): Future[ParameterList] = findByNameHead(name)

  def findAllParametersList(): Future[List[ParameterList]] = findAll()

  def findById(id: String): Future[ParameterList] = findByIdHead(id)

  def createFromParameterList(parameterList: ParameterList): Future[ParameterList] =
    db.run(filterById(parameterList.name).result).flatMap { parameterLists =>
      if (parameterLists.nonEmpty)
        throw new ServerException(s"Unable to create parameter list ${parameterList.name} because it already exists")
      else createAndReturn(addId(addCreationDate(parameterList)))
    }

  def createFromWorkflow(parameterListFromWorkflow: ParameterListFromWorkflow): Future[ParameterList] = {
    val parametersInWorkflow = WorkflowPostgresDao.getParametersUsed(parameterListFromWorkflow.workflow)
    val parametersVariables = parametersInWorkflow.flatMap { parameter =>
      if (!parameter.contains("env."))
        Option(ParameterVariable(parameter))
      else if (parameterListFromWorkflow.includeEnvironmentVariables)
        Option(ParameterVariable(parameter))
      else None
    }
    val newName = parameterListFromWorkflow.name.getOrElse {
      val dateCreation = new DateTime()
      s"AutoCreatedList_${parameterListFromWorkflow.workflow.name}_" +
        s"${dateCreation.toString("yyyy-MM-dd-HH:mm:ss:SSS")}"
    }
    val newParameterList = ParameterList(
      name = newName,
      description = parameterListFromWorkflow.description,
      tags = parameterListFromWorkflow.tags,
      parameters = parametersVariables
    )

    db.run(filterById(newParameterList.name).result).flatMap { parameterLists =>
      if (parameterLists.nonEmpty)
        throw new ServerException(s"Unable to create parameter list ${newParameterList.name} because it already exists")
      else createAndReturn(addId(addCreationDate(newParameterList)))
    }
  }

  def update(parameterList: ParameterList): Future[Unit] = {
    val newParameterList = addCreationDate(addUpdateDate(parameterList))
    val id = newParameterList.id.getOrElse(
      throw new ServerException(s"No parameter list found by id ${newParameterList.id}"))

    findById(id).flatMap { oldParameterList =>
      val workflowContextActions = if (newParameterList.parent.notBlank.isEmpty) {
        for {
          workflowsActions <- updateWorkflowsWithNewParamListName(oldParameterList.name, newParameterList.name)
          contextsActions <- updateContextsWithParent(oldParameterList, newParameterList)
        } yield workflowsActions ++ contextsActions
      } else Future(Seq.empty)

      workflowContextActions.flatMap { actions =>
        val actionsToExecute = actions :+ upsertAction(newParameterList)
        db.run(txHandler(DBIO.seq(actionsToExecute: _*).transactionally))
      }
    }
  }

  def deleteById(id: String): Future[Boolean] =
    for {
      parameterList <- findByIdHead(id)
      response <- deleteYield(Seq(parameterList))
    } yield response

  def deleteByName(name: String): Future[Boolean] =
    for {
      parameterList <- findByNameHead(name)
      response <- deleteYield(Seq(parameterList))
    } yield response

  def deleteAllParameterList(): Future[Boolean] =
    for {
      parametersLists <- findAll()
      response <- deleteYield(parametersLists)
    } yield response

  /** PRIVATE METHODS **/

  private[services] def deleteYield(parametersLists: Seq[ParameterList]): Future[Boolean] = {
    val updateDeleteActions = parametersLists.map { parameterList =>
      val workflowContextActions = if (parameterList.parent.notBlank.isEmpty) {
        for {
          workflowsActions <- updateWorkflowsWithNewParamListName(parameterList.name, "")
          contextsActions <- findByParent(parameterList.name).map(contexts =>
            contexts.map(context => deleteByIDAction(context.id.get))
          )
        } yield workflowsActions ++ contextsActions
      } else Future(Seq.empty)

      workflowContextActions.map { actions =>
        actions :+ deleteByIDAction(parameterList.id.get)
      }
    }

    Future.sequence(updateDeleteActions).map { actionsSequence =>
      val actions = actionsSequence.flatten
      Try(db.run(txHandler(DBIO.seq(actions: _*).transactionally))) match {
        case Success(_) =>
          log.info(s"Parameter lists ${parametersLists.map(_.name).mkString(",")} deleted")
          true
        case Failure(e) =>
          throw e
      }
    }
  }

  private[services] def findByIdHead(id: String): Future[ParameterList] =
    for {
      parameterList <- db.run(filterById(id).result)
    } yield {
      if (parameterList.nonEmpty)
        parameterList.head
      else throw new ServerException(s"No parameter list found by id $id")
    }

  private[services] def findByNameHead(name: String): Future[ParameterList] =
    for {
      parameterList <- db.run(table.filter(_.name === name).result)
    } yield {
      if (parameterList.nonEmpty)
        parameterList.head
      else throw new ServerException(s"No parameter list found by name $name")
    }

  private[services] def addId(parameterList: ParameterList): ParameterList =
    if (parameterList.id.notBlank.isEmpty)
      parameterList.copy(id = Some(UUID.randomUUID.toString))
    else parameterList

  private[services] def addCreationDate(parameterList: ParameterList): ParameterList =
    parameterList.creationDate match {
      case None => parameterList.copy(creationDate = Some(new DateTime()))
      case Some(_) => parameterList
    }

  private[services] def addUpdateDate(parameterList: ParameterList): ParameterList =
    parameterList.copy(lastUpdateDate = Some(new DateTime()))

  private[services] def updateContextsWithParent(
                                                  oldParent: ParameterList,
                                                  newParent: ParameterList
                                                ) = {
    findByParent(oldParent.name).map { contextLists =>
      contextLists.map { contextList =>
        val newVariables = newParent.parameters.map { parameter =>
          val oldContextParameterValue = contextList.getParameterValue(parameter.name).notBlank
          val oldParentParameterValue = oldParent.getParameterValue(parameter.name).notBlank
          val newParentParameterValue = newParent.getParameterValue(parameter.name).notBlank
          val newValue = (oldContextParameterValue, oldParentParameterValue, newParentParameterValue) match {
            case (oldContextValue, oldParentValue, _) if oldContextValue == oldParentValue =>
              newParentParameterValue
            case (Some(oldContextValue), Some(oldParentValue), _) if oldContextValue != oldParentValue =>
              oldContextParameterValue
            case (Some(_), None, _) =>
              oldContextParameterValue
            case (None, None, _) =>
              newParentParameterValue
            case _ =>
              newParentParameterValue
          }
          parameter.copy(value = newValue)
        }
        upsertAction(contextList.copy(
          parent = Option(newParent.name),
          parameters = newVariables
        ))
      }
    }
  }

  private[services] def updateWorkflowsWithNewParamListName(oldName: String, newName: String) = {
    if (oldName != newName) {
      workflowService.findAll().map { workflows =>
        val workflowsToUpdate = replaceWorkflowsWithNewParamListName(oldName, newName, workflows)
        workflowsToUpdate.map(workflow => workflowService.upsertAction(workflow))
      }
    } else Future(Seq.empty)
  }

  private[services] def replaceWorkflowsWithNewParamListName(
                                                              oldName: String,
                                                              newName: String,
                                                              workflows: Seq[Workflow]
                                                            ): Seq[Workflow] = {
    workflows.flatMap { workflow =>
      val workflowStr = write(workflow)
      if (workflowStr.contains(s"{{$oldName")) {
        val newNameToReplace = if (newName.nonEmpty) newName + "." else ""
        val newWorkflowStr = workflowStr.replaceAll(s"\\{\\{$oldName.", s"\\{\\{$newNameToReplace")
        val newWorkflow = read[Workflow](newWorkflowStr)
        Option(newWorkflow.copy(settings = newWorkflow.settings.copy(
          global = newWorkflow.settings.global.copy(
            parametersLists = newWorkflow.settings.global.parametersLists.flatMap { list =>
              if (list == oldName && newName.nonEmpty)
                Option(newName)
              else if (list == oldName && newName.isEmpty)
                None
              else Option(list)
            }
          )
        )))
      } else None
    }
  }

}