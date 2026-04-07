import { pickAssigneePickerErrorMessage } from '../components/AssigneePicker';
import { ApiProblemError } from '../lib/http';

describe('pickAssigneePickerErrorMessage', () => {
  it('prefers ApiProblemError detail', () => {
    const e = new ApiProblemError(400, { title: 'T', detail: 'D' });
    expect(pickAssigneePickerErrorMessage(e)).toBe('D');
  });

  it('uses ApiProblemError title when detail is absent', () => {
    const e = new ApiProblemError(400, { title: 'Server error' });
    expect(pickAssigneePickerErrorMessage(e)).toBe('Server error');
  });

  it('uses Error message', () => {
    expect(pickAssigneePickerErrorMessage(new Error('oops'))).toBe('oops');
  });

  it('falls back for unknown', () => {
    expect(pickAssigneePickerErrorMessage(42)).toBe('Something went wrong');
  });
});
