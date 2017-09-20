/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
  'use strict';

  angular
    .module('webApp')
    .factory('ModelFactory', ModelFactory);

  ModelFactory.$inject = ['UtilsService', 'modelConstants'];

  function ModelFactory(UtilsService, modelConstants) {
    var model = {};
    var error = {
      text: "",
      duplicatedOutput: false
    };
    var template = null;
    var context = {
      "position": null
    };
    var inputList = [];

    function init(newTemplate, order, position) {
      setPosition(position);
      template = newTemplate;
      model.outputFields = [];
      model.inputField = "";
      model.type = template.types[0].name;
      model.order = order;
      model.configuration = {};
      error.text = "";
      error.duplicatedOutput = false;
      if (model.type == modelConstants.DATETIME) {
        delete model.granularityNumber;
        delete model.granularityTime;
      }
    }

    function updateModelInputs(models) {
      inputList.length = 0;
      var index = context.position;
      inputList.push(template.defaultInput);
      var previousOutputs = generateOutputOptions(getPreviousOutputFields(models, index));
      inputList.push.apply(inputList, previousOutputs);

      if (!model.inputField) {
        model.inputField = inputList[0].value
      }
      return inputList;
    }

    function generateOutputOptions(outputs) {
      var options = [];
      var output, option = "";
      for (var i = 0; i < outputs.length; ++i) {
        output = outputs[i].name || outputs[i];
        option = {
          label: output,
          value: output
        };
        options.push(option);
      }
      return options;
    }

    function isValidModel() {
      var isValid = (model.inputField != "" || isAutoGeneratedDateTime() || model.type == modelConstants.CUSTOM);
      isValid = isValid && ((model.outputFields.length > 0 || (model.outputFields.length == 0 && model.type === modelConstants.FILTER)) || model.type === modelConstants.CUSTOM) &&
        model.type != "";
      if (!isValid) {
        error.text = "_ERROR_._GENERIC_FORM_";
      } else {
        error.text = "";
      }


      return isValid;
    }

    function getModel(template, order, position) {
      if (Object.keys(model).length == 0) {
        init(template, order, position);
      }
      return model;
    }

    function setModel(m, position) {
      model.outputFields = m.outputFields;
      model.type = m.type;
      model.configuration = m.configuration;
      model.inputField = m.inputField;
      model.order = m.order;
      if (m.type == modelConstants.DATETIME) {
        var granularity = m.configuration.granularity ? m.configuration.granularity.split(/([0-9]+)/) : '';
        model.configuration.granularityNumber = m.configuration.granularityNumber || granularity[1];
        model.configuration.granularityTime = m.configuration.granularityTime || granularity[2];
      }
      error.text = "";
      setPosition(position);
    }

    function resetModel(template, order, position) {
      init(template, order, position);
    }

    function getContext() {
      return context;
    }

    function setPosition(p) {
      if (p === undefined) {
        p = 0;
      }
      context.position = p;
    }

    function getError() {
      return error;
    }

    function setError(e) {
      error.text = e;
    }

    function getModelInputs() {
      return inputList;
    }

    function getPreviousOutputFields(models, index) {
      var outputFields = [];
      var i = 0;
      while (i < index && i < models.length) {
        var modelOutputFields = models[i].outputFields;
        for (var j = 0; j < modelOutputFields.length; ++j) {
          var outputField = modelOutputFields[j];

          if (UtilsService.findElementInJSONArray(outputFields, outputField, 'name') == -1) {
            outputFields.push(outputField);
          }
        }
        ++i;
      }
      return outputFields;
    }

    function getOutputFields(models, index) {
      var outputFields = [];

      for (var i = 0; i < models.length; i++) {
        var modelOutputFields = models[i].outputFields;
        if (i !== index) {
          for (var j = 0; j < modelOutputFields.length; ++j) {
            var outputField = modelOutputFields[j];

            if (UtilsService.findElementInJSONArray(outputFields, outputField, 'name') == -1) {
              outputFields.push(outputField);
            }
          }
        }
      }
      return outputFields;
    }

    function isAutoGeneratedDateTime() {
      return (model.type === modelConstants.DATETIME && model.configuration.formatFrom === modelConstants.AUTOGENERATED);
    }

    function isFilterModel() {
      return (model.type === modelConstants.FILTER);
    }

    return {
      resetModel: resetModel,
      getModel: getModel,
      setModel: setModel,
      getContext: getContext,
      setPosition: setPosition,
      isValidModel: isValidModel,
      getError: getError,
      setError: setError,
      getModelInputs: getModelInputs,
      updateModelInputs: updateModelInputs,
      isAutoGeneratedDateTime: isAutoGeneratedDateTime,
      getPreviousOutputFields: getPreviousOutputFields,
      getOutputFields: getOutputFields,
      isFilterModel: isFilterModel
    }
  }
})
();
