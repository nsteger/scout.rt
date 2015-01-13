/* global FormSpecHelper */
describe("StringField", function() {
  var session;
  var helper;

  beforeEach(function() {
    setFixtures(sandbox());
    session = new scout.Session($('#sandbox'), '1.1');
    helper = new FormSpecHelper(session);
  });

  function createField(model) {
    var field = new scout.StringField();
    field.init(model, session);
    return field;
  }

  function createModel() {
    return helper.createFieldModel();
  }

  describe("property label position", function() {
    var field;

    beforeEach(function() {
      field = createField(createModel());
    });

    describe("position on_field", function() {

      beforeEach(function() {
        field.label = 'labelName';
        field.labelPosition = scout.FormField.LABEL_POSITION_ON_FIELD;
      });

      it("sets the label as placeholder", function() {
        field.render(session.$entryPoint);
        expect(field.$label.html()).toBeFalsy();
        expect(field.$field.attr('placeholder')).toBe(field.label);
      });

      it("does not call field._renderLabelPosition initially", function() {
        field.render(session.$entryPoint);
        expect(field.$label.html()).toBeFalsy();
        expect(field.$field.attr('placeholder')).toBe(field.label);

        spyOn(field, '_renderLabelPosition');
        expect(field._renderLabelPosition).not.toHaveBeenCalled();
      });

    });

  });


  describe("Check if field is switched to password field if inputMasked is true", function() {
    var field;

    beforeEach(function() {
      field = createField(createModel());
    });

      it("set input masked", function() {
        field.inputMasked = true;
        field.render(session.$entryPoint);
        expect(field.$field.attr('type')).toBe('password');
      });

      it("set input not masked", function() {
        field.inputMasked = false;
        field.render(session.$entryPoint);
        expect(field.$field.attr('type')).toBe('text');
      });


  });

});
