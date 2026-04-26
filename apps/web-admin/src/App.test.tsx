import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

describe('App', () => {
  it('renders resident profile workspace', () => {
    render(<App />);

    expect(screen.getByText('长者档案管理')).toBeInTheDocument();
    expect(screen.getByText('陈兰英')).toBeInTheDocument();
  });

  it('disables create action for property supervisor role', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getAllByRole('combobox')[0]);
    await user.click(screen.getByText('物业主管 · 物业部门'));

    expect(screen.getByRole('button', { name: /新建档案/ })).toBeDisabled();
  });

  it('supports runtime light and dark theme switching', async () => {
    const user = userEvent.setup();
    const { container } = render(<App />);

    expect(container.querySelector('.app-shell')).toHaveClass('dark-theme');

    await user.click(screen.getByRole('button', { name: '切换主题' }));

    expect(container.querySelector('.app-shell')).toHaveClass('light-theme');
  });
});
