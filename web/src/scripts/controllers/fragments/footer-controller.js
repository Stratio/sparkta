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
(function() {
  'use strict';

  /*EDIT FRAGMENT MODAL CONTROLLER*/
  angular
      .module('webApp')
      .controller('FooterCtrl', FooterCtrl);

  FooterCtrl.$inject = ['ModalService'];

  function FooterCtrl(ModalService) {
    /*jshint validthis: true*/
    var vm = this;
    vm.showLegalTermsModal  = showLegalTermsModal;

    /////////////////////////////////


    function showLegalTermsModal(){
        var controller = "LegalTermsModalCtrl";
        var templateUrl = "templates/modal/legal-terms-modal.tpl.html";
        var resolve = {};
        ModalService.openModal(controller, templateUrl, resolve, '', 'lg');

    }

  }
})();